import pytest

from context import (
    Context,
    ContextOverflow,
    Turn,
    apply_memory,
    check_turns,
    clear_tool_results,
    compact,
    context_tokens,
    estimate_tokens,
    render_system,
    split_point,
)

SYSTEM = "You are a coding agent. Fix the failing test."


def user(text, pinned=False):
    return Turn("user", text, pinned=pinned)


def call(call_id, text="calling a tool"):
    return Turn("assistant", text, call_id=call_id)


def result(call_id, text):
    return Turn("tool", text, call_id=call_id)


def history():
    return [
        user("Fix test_parse. Do not change the public API.", pinned=True),
        call("c1", "read parser.py"),
        result("c1", "def parse(): ..." * 20),
        call("c2", "run the tests"),
        result("c2", "FAILED test_parse " * 20),
        Turn("assistant", "The bug is in parse(); the fix is a missing strip()."),
        user("Go ahead."),
        call("c3", "edit parser.py"),
        result("c3", "ok"),
        call("c4", "run the tests"),
        result("c4", "1 passed"),
    ]


class Summariser:
    def __init__(self, text="Read parser.py; the bug is a missing strip()."):
        self.text = text
        self.calls = []

    def __call__(self, turns):
        self.calls.append(turns)
        return self.text


# ---- tokens and shape ------------------------------------------------------------


def test_context_tokens_counts_system_and_turns():
    assert estimate_tokens("123456789") == 3
    assert context_tokens("abcd", [user("12345678"), result("x", "1")]) == 1 + 2 + 1


def test_a_valid_history_has_no_problems():
    assert check_turns(history()) == []
    assert check_turns([]) == []


def test_check_turns_finds_broken_pairs_and_bad_pins():
    turns = [
        Turn("system", "x"),
        result("c9", "orphan"),
        call("c1"),
        user("interrupts the call"),
        Turn("assistant", "pinned", pinned=True),
        call("c2"),
    ]
    assert check_turns(turns) == [
        "turn 0: unknown role 'system'",
        "turn 1: tool result does not follow its call",
        "turn 2: call 'c1' has no result",
        "turn 4: only user turns can be pinned",
        "turn 5: call 'c2' has no result",
    ]


def test_a_result_with_the_wrong_id_does_not_follow_its_call():
    assert check_turns([call("c1"), result("c2", "x")]) == ["turn 1: tool result does not follow its call"]


# ---- clearing tool results -------------------------------------------------------


def test_clear_keeps_the_last_results_and_pairs():
    turns = history()
    cleared, ids = clear_tool_results(turns, keep_last=2)
    assert ids == ["c1", "c2"]
    assert cleared[2] == result("c1", f"[result cleared: {estimate_tokens(turns[2].content)} tokens]")
    assert cleared[4].call_id == "c2"
    assert cleared[8:] == turns[8:]
    assert [turn.role for turn in cleared] == [turn.role for turn in turns]
    assert check_turns(cleared) == []


def test_clear_does_not_change_its_input_and_skips_placeholders():
    turns = history()
    snapshot = list(turns)
    once, _ = clear_tool_results(turns, keep_last=1)
    assert turns == snapshot
    twice, ids = clear_tool_results(once, keep_last=1)
    assert twice == once
    assert ids == []


def test_keep_last_zero_and_more_than_there_are():
    turns = history()
    assert clear_tool_results(turns, keep_last=0)[1] == ["c1", "c2", "c3", "c4"]
    assert clear_tool_results(turns, keep_last=10) == (turns, [])


# ---- the split point -------------------------------------------------------------


def test_split_point_never_starts_on_a_tool_result():
    turns = history()
    assert split_point(turns, 2) == 9
    assert split_point(turns, 1) == 9
    assert split_point(turns, 5) == 6
    assert split_point(turns, 50) == 0


# ---- compacting ------------------------------------------------------------------


def test_a_context_that_fits_is_returned_unchanged():
    turns = history()
    summarise = Summariser()
    context = compact(SYSTEM, turns, budget_tokens=10_000, keep_recent=2, keep_tool_results=1, summarise=summarise)
    assert context == Context(SYSTEM, turns, context_tokens(SYSTEM, turns), [], 0)
    assert summarise.calls == []


def test_clearing_comes_before_summarising():
    turns = history()
    cleared, _ = clear_tool_results(turns, keep_last=2)
    budget = context_tokens(SYSTEM, cleared)
    summarise = Summariser()
    context = compact(SYSTEM, turns, budget_tokens=budget, keep_recent=2, keep_tool_results=2, summarise=summarise)
    assert context.turns == cleared
    assert (context.cleared, context.summarised, context.tokens) == (["c1", "c2"], 0, budget)
    assert summarise.calls == []


def test_compaction_keeps_pinned_turns_a_summary_and_the_recent_turns():
    turns = history()
    summarise = Summariser("Read parser.py & found <the> bug.")
    context = compact(SYSTEM, turns, budget_tokens=60, keep_recent=2, keep_tool_results=2, summarise=summarise)
    assert context.turns == [
        turns[0],
        Turn("user", "<summary>\nRead parser.py &amp; found &lt;the&gt; bug.\n</summary>"),
        call("c4", "run the tests"),
        result("c4", "1 passed"),
    ]
    assert context.summarised == 8
    assert context.cleared == ["c1", "c2"]
    assert context.tokens == context_tokens(SYSTEM, context.turns) <= 60
    assert check_turns(context.turns) == []


def test_the_summariser_gets_the_unpinned_old_turns_after_clearing():
    turns = history()
    summarise = Summariser()
    compact(SYSTEM, turns, budget_tokens=60, keep_recent=2, keep_tool_results=2, summarise=summarise)
    (sent,) = summarise.calls
    assert sent[0] == turns[1]
    assert sent[1].content.startswith("[result cleared: ")
    assert len(sent) == 8
    assert not any(turn.pinned for turn in sent)


def test_overflow_when_the_summary_does_not_help_enough():
    with pytest.raises(ContextOverflow) as error:
        compact(SYSTEM, history(), budget_tokens=30, keep_recent=2, keep_tool_results=2, summarise=Summariser())
    assert error.value.budget == 30
    assert error.value.tokens > 30
    assert str(error.value) == f"context needs {error.value.tokens} tokens, budget is 30"


def test_overflow_when_there_is_nothing_old_to_summarise():
    turns = [user("x" * 400, pinned=True), user("y" * 400)]
    with pytest.raises(ContextOverflow):
        compact(SYSTEM, turns, budget_tokens=50, keep_recent=1, keep_tool_results=0, summarise=Summariser())


def test_compact_rejects_a_broken_history_before_doing_anything():
    summarise = Summariser()
    with pytest.raises(ValueError):
        compact(SYSTEM, [call("c1"), user("x")], budget_tokens=1, keep_recent=1, keep_tool_results=0, summarise=summarise)
    assert summarise.calls == []


# ---- memory ----------------------------------------------------------------------


def test_apply_memory_adds_replaces_and_deletes_without_mutating():
    notes = {"api": "public API is frozen", "todo": "fix parse"}
    reply = (
        'Done. <memory key="todo"> run the full suite </memory>\n'
        '<memory key="api"></memory>\n'
        '<memory key="root-cause">a missing strip() &amp; no test</memory>'
    )
    assert apply_memory(notes, reply) == {"todo": "run the full suite", "root-cause": "a missing strip() & no test"}
    assert notes == {"api": "public API is frozen", "todo": "fix parse"}


def test_apply_memory_rejects_bad_keys():
    with pytest.raises(ValueError):
        apply_memory({}, '<memory key="Bad Key">x</memory>')
    with pytest.raises(ValueError):
        apply_memory({}, '<memory key="">x</memory>')


def test_render_system_sorts_and_escapes_notes():
    assert render_system("  Base.  ", {}) == "Base."
    assert render_system("Base.", {"zeta": "a < b", "alpha": "x"}) == (
        'Base.\n\n<memory>\n<note key="alpha">x</note>\n<note key="zeta">a &lt; b</note>\n</memory>'
    )
