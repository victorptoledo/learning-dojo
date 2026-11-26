import pytest

from optimiser import (
    Example,
    Failure,
    Outcome,
    Round,
    extract_candidate,
    optimise,
    placeholders,
    render,
    render_metaprompt,
    score,
)

SEED = "Classify the support ticket as billing or technical.\n<ticket>{{input}}</ticket>"
ONLY_INVOICE = SEED + "\nRULE invoice=billing"
INVOICE_AND_REFUND = ONLY_INVOICE + "\nRULE refund=billing"
MEMORISED = (
    SEED
    + "\nRULE my invoice is wrong=billing\nRULE refund please=billing"
    + "\nRULE the app crashes=technical\nRULE login fails=technical\nDEFAULT billing"
)

DEV = [
    Example("d1", "my invoice is wrong", "billing"),
    Example("d2", "the app crashes", "technical"),
    Example("d3", "refund please", "billing"),
    Example("d4", "login fails", "technical"),
]
HOLDOUT = [
    Example("h1", "need an invoice copy", "billing"),
    Example("h2", "refund status?", "billing"),
    Example("h3", "screen frozen", "technical"),
    Example("h4", "cannot connect", "technical"),
    Example("h5", "upload error", "technical"),
]


def model(messages):
    """A fake classifier that follows the RULE and DEFAULT lines of the prompt it gets."""
    assert len(messages) == 1 and messages[0]["role"] == "user"
    text = messages[0]["content"]
    ticket = text.split("<ticket>", 1)[1].split("</ticket>", 1)[0]
    default = "technical"
    for line in text.splitlines():
        if line.startswith("RULE "):
            word, label = line[5:].split("=")
            if word in ticket:
                return f" {label}\n"
        elif line.startswith("DEFAULT "):
            default = line[8:]
    return default


def grader(got, expected):
    return got.casefold() == expected.casefold()


class Optimiser:
    """A fake optimiser model that returns recorded replies in order and keeps every call."""

    def __init__(self, *replies):
        self.replies = list(replies)
        self.calls = []

    def __call__(self, messages):
        self.calls.append(messages)
        return self.replies.pop(0)


def proposal(template):
    escaped = template.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return f"<analysis>Tickets about money are billing.</analysis>\n<prompt>\n{escaped}\n</prompt>"


# ---- templates -------------------------------------------------------------------


def test_placeholders_are_sorted_and_unique():
    assert placeholders("{{b}} {{a}} {{b}} {{ not_one }} {{Upper}}") == ["a", "b"]


def test_render_escapes_values_and_needs_every_placeholder():
    assert render("<t>{{input}}</t> {{input}}", {"input": "a < b"}) == "<t>a &lt; b</t> a &lt; b"
    with pytest.raises(ValueError) as error:
        render("{{input}} {{tone}}", {"input": "x"})
    assert str(error.value) == "no value for {{tone}}"


def test_score_counts_passes_and_keeps_stripped_failures():
    result = score(SEED, DEV, model, grader)
    assert result.rate == 0.5
    assert result.failures == [
        Failure("d1", "my invoice is wrong", "billing", "technical"),
        Failure("d3", "refund please", "billing", "technical"),
    ]
    assert score(ONLY_INVOICE, DEV, model, grader).rate == 0.75


def test_score_sends_one_rendered_user_message_per_example():
    calls = []

    def recording(messages):
        calls.append(messages)
        return "billing"

    score(SEED, [Example("x", "a & b", "billing")], recording, grader)
    assert calls == [[{"role": "user", "content": SEED.replace("{{input}}", "a &amp; b")}]]
    with pytest.raises(ValueError):
        score(SEED, [], recording, grader)


# ---- the metaprompt --------------------------------------------------------------


def test_metaprompt_layout_with_escaping_and_order():
    failures = [
        Failure("f2", "two", "billing", "technical"),
        Failure("f1", "a <b>", "billing", "x & y"),
    ]
    text = render_metaprompt("Say <t>{{input}}</t>", failures, max_failures=5, max_chars=100)
    assert text == (
        "You improve prompt templates. Below are the current template and examples it got wrong.\n\n"
        "<current_prompt>\nSay &lt;t&gt;{{input}}&lt;/t&gt;\n</current_prompt>\n\n"
        "<failures>\n"
        '<failure id="f1">\n<input>a &lt;b&gt;</input>\n<expected>billing</expected>\n<got>x &amp; y</got>\n</failure>\n'
        '<failure id="f2">\n<input>two</input>\n<expected>billing</expected>\n<got>technical</got>\n</failure>\n'
        "</failures>\n\n"
        "Keep every placeholder of the template exactly as written. Explain what went wrong inside "
        "<analysis> tags, then write the complete new template inside <prompt> tags."
    )


def test_metaprompt_caps_the_failures_and_clips_long_fields():
    failures = [Failure(f"f{n}", "x" * 50, "billing", "<" * 20) for n in range(9, 0, -1)]
    text = render_metaprompt("{{input}}", failures, max_failures=3, max_chars=10)
    assert [f'id="f{n}"' in text for n in range(1, 10)] == [True] * 3 + [False] * 6
    assert "<input>xxxxxxxxxx [truncated]</input>" in text
    assert "<got>" + "&lt;" * 10 + " [truncated]</got>" in text
    assert "<expected>billing</expected>" in text


def test_metaprompt_needs_failures():
    with pytest.raises(ValueError):
        render_metaprompt("{{input}}", [], max_failures=5, max_chars=100)


# ---- extracting candidates -------------------------------------------------------


def test_extract_takes_the_last_prompt_unescaped_and_stripped():
    reply = "<prompt>draft {{input}}</prompt> then <prompt>\n  final &lt;t&gt;{{input}}&lt;/t&gt;  \n</prompt>"
    assert extract_candidate(reply, ["input"]) == "final <t>{{input}}</t>"


def test_extract_errors_name_the_problem():
    with pytest.raises(ValueError, match="^no <prompt> in the reply$"):
        extract_candidate("Here is a better prompt: {{input}}", ["input"])
    with pytest.raises(ValueError, match="^the candidate is empty$"):
        extract_candidate("<prompt>  </prompt>", ["input"])
    with pytest.raises(ValueError) as error:
        extract_candidate("<prompt>Use {{tone}} and {{ticket}}</prompt>", ["input"])
    assert str(error.value) == "missing {{input}}; unknown placeholder {{ticket}}; unknown placeholder {{tone}}"


# ---- the loop --------------------------------------------------------------------


def test_the_loop_accepts_better_candidates_and_stops_when_nothing_fails():
    optimiser = Optimiser(proposal(ONLY_INVOICE), proposal(INVOICE_AND_REFUND), proposal("never used {{input}}"))
    outcome = optimise(SEED, dev=DEV, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader, rounds=5)
    assert outcome == Outcome(
        INVOICE_AND_REFUND,
        1.0,
        1.0,
        [Round(1, "accepted", 0.75, ""), Round(2, "accepted", 1.0, "")],
        True,
        "dev 50% -> 100%, holdout 60% -> 100%",
    )
    assert len(optimiser.calls) == 2


def test_each_round_starts_from_a_fresh_context_with_the_current_best():
    optimiser = Optimiser(proposal(ONLY_INVOICE), proposal(INVOICE_AND_REFUND))
    optimise(SEED, dev=DEV, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader, rounds=2)
    first, second = optimiser.calls
    assert len(first) == len(second) == 1
    assert first[0]["role"] == second[0]["role"] == "user"
    assert 'id="d1"' in first[0]["content"] and 'id="d3"' in first[0]["content"]
    assert "RULE invoice=billing" in second[0]["content"]
    assert 'id="d1"' not in second[0]["content"] and 'id="d3"' in second[0]["content"]


def test_the_optimiser_never_sees_the_holdout_set():
    optimiser = Optimiser(proposal(ONLY_INVOICE), proposal(INVOICE_AND_REFUND))
    optimise(SEED, dev=DEV, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader, rounds=2)
    sent = "".join(messages[0]["content"] for messages in optimiser.calls)
    assert not any(example.input in sent or f'"{example.id}"' in sent for example in HOLDOUT)


def test_invalid_and_unchanged_candidates_are_logged_and_the_loop_goes_on():
    optimiser = Optimiser(
        "<prompt>Classify: <ticket>{{text}}</ticket></prompt>",
        "I would change nothing.",
        proposal(SEED),
        proposal(INVOICE_AND_REFUND),
    )
    outcome = optimise(SEED, dev=DEV, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader, rounds=4)
    assert outcome.rounds == [
        Round(1, "invalid", None, "missing {{input}}; unknown placeholder {{text}}"),
        Round(2, "invalid", None, "no <prompt> in the reply"),
        Round(3, "invalid", None, "the candidate is unchanged"),
        Round(4, "accepted", 1.0, ""),
    ]
    assert outcome.shipped


def test_a_candidate_must_beat_the_best_by_more_than_the_margin():
    same = SEED + "\nRULE nothing=billing"
    optimiser = Optimiser(proposal(same), proposal(ONLY_INVOICE))
    outcome = optimise(
        SEED, dev=DEV, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader, rounds=2, min_gain=0.25
    )
    assert outcome.rounds == [Round(1, "rejected", 0.5, "no gain over 50%"), Round(2, "rejected", 0.75, "no gain over 50%")]
    assert (outcome.prompt, outcome.shipped, outcome.reason) == (SEED, False, "no candidate beat the seed")
    assert (outcome.dev_rate, outcome.holdout_rate) == (0.5, 0.6)


def test_a_candidate_that_memorised_the_dev_set_is_not_shipped():
    optimiser = Optimiser(proposal(MEMORISED))
    outcome = optimise(SEED, dev=DEV, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader, rounds=3)
    assert outcome.rounds == [Round(1, "accepted", 1.0, "")]
    assert outcome == Outcome(SEED, 0.5, 0.6, outcome.rounds, False, "overfit: holdout fell from 60% to 40%")


def test_an_equal_holdout_score_still_ships():
    optimiser = Optimiser(proposal(SEED + "\nRULE wrong=billing\nRULE please=billing"))
    outcome = optimise(SEED, dev=DEV, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader, rounds=1)
    assert (outcome.shipped, outcome.dev_rate, outcome.holdout_rate) == (True, 1.0, 0.6)


def test_a_perfect_seed_needs_no_optimiser():
    optimiser = Optimiser()
    outcome = optimise(
        INVOICE_AND_REFUND, dev=DEV, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader, rounds=3
    )
    assert outcome == Outcome(INVOICE_AND_REFUND, 1.0, 1.0, [], False, "nothing to improve")
    assert optimiser.calls == []


def test_the_loop_respects_the_caps_it_is_given():
    long_dev = [Example(f"d{n}", "invoice " + "z" * 40, "billing") for n in range(1, 8)]
    optimiser = Optimiser(proposal(ONLY_INVOICE))
    optimise(
        SEED, dev=long_dev, holdout=HOLDOUT, model=model, optimiser=optimiser, grader=grader,
        rounds=1, max_failures=2, max_chars=12,
    )
    sent = optimiser.calls[0][0]["content"]
    assert sent.count("<failure ") == 2
    assert "<input>invoice zzzz [truncated]</input>" in sent


def test_bad_setups_are_rejected_before_any_call():
    optimiser = Optimiser()
    calls = []

    def counting(messages):
        calls.append(messages)
        return "billing"

    with pytest.raises(ValueError):
        optimise(SEED, dev=DEV, holdout=HOLDOUT + [DEV[0]], model=counting, optimiser=optimiser, grader=grader, rounds=1)
    with pytest.raises(ValueError):
        optimise("Classify {{ticket}}", dev=DEV, holdout=HOLDOUT, model=counting, optimiser=optimiser, grader=grader, rounds=1)
    with pytest.raises(ValueError):
        optimise("Classify {{input}} in {{tone}}", dev=DEV, holdout=HOLDOUT, model=counting, optimiser=optimiser, grader=grader, rounds=1)
    assert calls == [] and optimiser.calls == []
