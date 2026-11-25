import ast
import dataclasses
import time
from pathlib import Path

import pytest

from checker import (
    ProgramError,
    State,
    StateSpaceTooLarge,
    enabled,
    explore,
    initial_state,
    is_final,
    mutual_exclusion,
    replay,
    step,
    validate,
)

INCREMENT = [("load", "r", "x"), ("add", "r", 1), ("store", "x", "r")]
LOCKED_INCREMENT = [("lock", "m"), *INCREMENT, ("unlock", "m")]
CAS_INCREMENT = [
    ("load", "old", "x"),
    ("set", "new", "old"),
    ("add", "new", 1),
    ("cas", "ok", "x", "old", "new"),
    ("jump_if", "ok", "==", 0, 0),
]
NAIVE_LOCK = [
    ("load", "r", "busy"),
    ("jump_if", "r", "!=", 0, 0),
    ("store", "busy", 1),
    ("critical",),
    ("store", "busy", 0),
]


def locks_in_order(first, second):
    return [("lock", first), ("lock", second), ("critical",), ("unlock", second), ("unlock", first)]


def peterson(me, other):
    return [
        ("store", f"flag{me}", 1),
        ("store", "turn", other),
        ("load", "f", f"flag{other}"),
        ("jump_if", "f", "==", 0, 6),
        ("load", "t", "turn"),
        ("jump_if", "t", "!=", me, 2),
        ("critical",),
        ("store", f"flag{me}", 0),
    ]


def run(threads, shared, schedule):
    return replay(threads, shared, schedule)


# ---- instructions -----------------------------------------------------------------------

def test_initial_state():
    state = initial_state([INCREMENT, INCREMENT, INCREMENT], {"y": 7, "x": 1})
    assert state.pcs == (0, 0, 0)
    assert state.value("x") == 1 and state.value("y") == 7
    assert state.register(2, "r") == 0
    assert state.holder("m") is None


def test_load_add_store():
    threads = [INCREMENT]
    state = run(threads, {"x": 41}, [0, 0])
    assert state.register(0, "r") == 42 and state.value("x") == 41
    state = step(threads, state, 0)
    assert state.value("x") == 42 and is_final(threads, state)


def test_store_and_set_take_a_literal_or_a_register():
    threads = [[("set", "a", 5), ("set", "b", "a"), ("add", "b", "a"), ("store", "x", "b"), ("store", "y", -3)]]
    state = run(threads, {"x": 0, "y": 0}, [0] * 5)
    assert state.register(0, "b") == 10
    assert state.value("x") == 10 and state.value("y") == -3


def test_cas_succeeds_and_fails():
    threads = [[("cas", "ok", "x", 0, 9), ("cas", "again", "x", 0, 5)]]
    state = run(threads, {"x": 0}, [0])
    assert state.value("x") == 9 and state.register(0, "ok") == 1
    state = step(threads, state, 0)
    assert state.value("x") == 9 and state.register(0, "again") == 0


@pytest.mark.parametrize("cmp, value, jumps", [
    ("==", 3, True), ("==", 4, False), ("!=", 4, True), ("!=", 3, False),
    ("<", 4, True), ("<", 3, False), (">=", 3, True), (">=", 4, False),
])
def test_jump_if(cmp, value, jumps):
    threads = [[("set", "r", 3), ("jump_if", "r", cmp, value, 3), ("critical",), ("critical",)]]
    state = run(threads, {}, [0, 0])
    assert state.pc(0) == (3 if jumps else 2)


def test_jump_compares_with_a_register_and_can_finish_the_thread():
    threads = [[("set", "a", 2), ("set", "b", 2), ("jump_if", "a", "==", "b", 4), ("critical",)]]
    assert is_final(threads, run(threads, {}, [0, 0, 0]))
    threads = [[("jump", 2), ("critical",)]]
    assert is_final(threads, run(threads, {}, [0]))


def test_lock_blocks_other_threads_only_while_held():
    threads = [[("lock", "m"), ("unlock", "m")], [("lock", "m")]]
    state = run(threads, {}, [0])
    assert state.holder("m") == 0
    assert enabled(threads, state) == [0]
    state = step(threads, state, 0)
    assert state.holder("m") is None
    assert enabled(threads, state) == [1]


def test_unlocking_a_lock_you_do_not_hold_is_an_error():
    threads = [[("lock", "m")], [("unlock", "m")]]
    with pytest.raises(ProgramError):
        step(threads, initial_state(threads, {}), 1)
    state = run(threads, {}, [0])
    with pytest.raises(ProgramError):
        step(threads, state, 1)


def test_step_does_not_change_the_given_state():
    threads = [INCREMENT]
    start = initial_state(threads, {"x": 5})
    first = step(threads, start, 0)
    step(threads, first, 0)
    assert start.pcs == (0,) and first.register(0, "r") == 5 and first.pcs == (1,)


def test_equal_states_are_equal_whatever_the_write_order():
    threads = [[("store", "a", 1)], [("store", "b", 1)]]
    left = run(threads, {"a": 0, "b": 0}, [0, 1])
    right = run(threads, {"b": 0, "a": 0}, [1, 0])
    assert left == right and hash(left) == hash(right)


def test_enabled_skips_finished_threads():
    threads = [[("critical",)], [("critical",), ("critical",)]]
    assert enabled(threads, run(threads, {}, [0])) == [1]
    assert enabled(threads, run(threads, {}, [0, 1, 1])) == []


def test_replay_refuses_a_thread_that_cannot_run():
    threads = [[("lock", "m")], [("lock", "m")]]
    with pytest.raises(ValueError):
        replay(threads, {}, [0, 1])
    with pytest.raises(ValueError):
        replay([[("critical",)]], {}, [0, 0])


# ---- validate ---------------------------------------------------------------------------

@pytest.mark.parametrize("thread", [
    [("pop", "r")],
    [("load", "r")],
    [("add", "r", 1, 2)],
    [("load", "r", "nope")],
    [("store", "nope", 1)],
    [("cas", "ok", "nope", 0, 1)],
    [("jump_if", "r", "<=", 0, 0)],
    [("jump", 3)],
    [("jump", -1)],
    [("jump_if", "r", "==", 0, 3)],
    [()],
])
def test_validate_rejects_bad_programs(thread):
    program = [[("critical",)], [("critical",), *thread]]
    with pytest.raises(ValueError, match=r"thread 1, instruction 1"):
        validate(program, {"x": 0})
    with pytest.raises(ValueError):
        explore(program, {"x": 0})


def test_validate_accepts_good_programs():
    validate([INCREMENT, CAS_INCREMENT, NAIVE_LOCK, [("jump", 1)]], {"x": 0, "busy": 0})


# ---- explore ----------------------------------------------------------------------------

def test_lost_update():
    report = explore([INCREMENT, INCREMENT], {"x": 0})
    assert report.final_values("x") == {1, 2}
    assert report.violation is None and report.deadlock is None


def test_a_lock_fixes_the_lost_update():
    report = explore([LOCKED_INCREMENT] * 3, {"x": 0})
    assert report.final_values("x") == {3}
    assert report.deadlock is None and report.violation is None


def test_a_cas_loop_fixes_the_lost_update():
    report = explore([CAS_INCREMENT] * 3, {"x": 0})
    assert report.final_values("x") == {3}
    assert report.deadlock is None


def test_locks_in_opposite_orders_deadlock():
    threads = [locks_in_order("a", "b"), locks_in_order("b", "a")]
    report = explore(threads, {})
    assert report.deadlock is not None and len(report.deadlock) == 2
    stuck = replay(threads, {}, report.deadlock)
    assert enabled(threads, stuck) == [] and not is_final(threads, stuck)
    assert report.violation is None and report.finals


def test_lock_ordering_removes_the_deadlock():
    threads = [locks_in_order("a", "b"), locks_in_order("a", "b")]
    report = explore(threads, {}, mutual_exclusion(threads))
    assert report.deadlock is None and report.violation is None
    assert len(report.finals) == 1


def test_a_thread_waiting_for_its_own_lock_deadlocks():
    threads = [[("lock", "m"), ("lock", "m")]]
    assert explore(threads, {}).deadlock == [0]


def test_a_naive_flag_lock_breaks_mutual_exclusion():
    threads = [NAIVE_LOCK, NAIVE_LOCK]
    report = explore(threads, {"busy": 0}, mutual_exclusion(threads))
    assert report.violation is not None and len(report.violation) == 6
    assert not mutual_exclusion(threads)(replay(threads, {"busy": 0}, report.violation))


def test_peterson_keeps_mutual_exclusion_without_deadlock():
    threads = [peterson(0, 1), peterson(1, 0)]
    shared = {"flag0": 0, "flag1": 0, "turn": 0}
    report = explore(threads, shared, mutual_exclusion(threads))
    assert report.violation is None and report.deadlock is None
    assert report.final_values("flag0") == {0} and report.final_values("flag1") == {0}


def test_peterson_without_the_turn_variable_can_deadlock_or_spin():
    def flags_only(me, other):
        return [("store", f"flag{me}", 1), ("load", "f", f"flag{other}"),
                ("jump_if", "f", "!=", 0, 1), ("critical",), ("store", f"flag{me}", 0)]
    threads = [flags_only(0, 1), flags_only(1, 0)]
    report = explore(threads, {"flag0": 0, "flag1": 0}, mutual_exclusion(threads))
    assert report.violation is None
    # Both flags up, both spinning: the threads can still run, so this is livelock, not deadlock.
    assert report.deadlock is None
    spinning = replay(threads, {"flag0": 0, "flag1": 0}, [0, 1, 0, 1])
    assert enabled(threads, spinning) == [0, 1]


def test_mutual_exclusion_counts_threads_at_critical():
    threads = [[("critical",)], [("critical",)], [("store", "x", 1)]]
    invariant = mutual_exclusion(threads)
    both = initial_state(threads, {"x": 0})
    assert not invariant(both)
    assert invariant(step(threads, both, 0))


def test_the_reported_schedule_is_a_shortest_one():
    # The long way round: thread 0 sets x after 20 steps; thread 1 sets it at once.
    threads = [[("add", "i", 1)] * 20 + [("store", "x", 1)], [("store", "x", 1)]]
    report = explore(threads, {"x": 0}, lambda state: state.value("x") == 0)
    assert report.violation == [1]


def test_a_program_error_is_a_violation_with_its_schedule():
    threads = [[("critical",), ("critical",)], [("lock", "m"), ("critical",), ("unlock", "n")]]
    report = explore(threads, {})
    assert report.violation == [1, 1, 1]
    with pytest.raises(ProgramError):
        replay(threads, {}, report.violation)


def test_exploration_goes_on_after_a_violation():
    report = explore([INCREMENT, INCREMENT], {"x": 0}, lambda state: state.value("x") != 1)
    assert report.violation is not None and len(report.violation) == 3
    assert report.final_values("x") == {1, 2}


def test_the_initial_state_can_break_the_invariant():
    assert explore([INCREMENT], {"x": 0}, lambda state: False).violation == []


@pytest.mark.parametrize("threads, steps", [(1, 7), (2, 5), (3, 4), (5, 3)])
def test_repeated_states_are_merged(threads, steps):
    program = [[("add", "r", 1)] * steps] * threads
    report = explore(program, {})
    assert report.states == (steps + 1) ** threads
    assert len(report.finals) == 1


def test_a_deep_program_does_not_recurse():
    loop = [("load", "i", "n"), ("add", "i", 1), ("store", "n", "i"), ("jump_if", "i", "<", 3000, 0)]
    report = explore([loop], {"n": 0})
    assert report.states == 12_001
    assert report.final_values("n") == {3000}


def test_sixteen_thousand_states_in_a_few_seconds():
    started = time.perf_counter()
    report = explore([[("add", "r", 1)] * 4] * 6, {})
    assert report.states == 5 ** 6
    assert time.perf_counter() - started < 8


def test_max_states_stops_a_state_explosion():
    started = time.perf_counter()
    with pytest.raises(StateSpaceTooLarge):
        explore([[("add", "r", 1)] * 10] * 8, {}, max_states=5_000)
    assert time.perf_counter() - started < 5
    assert explore([[("add", "r", 1)] * 3] * 2, {}, max_states=16).states == 16


# ---- structure --------------------------------------------------------------------------

SOURCE = Path("checker.py").read_text(encoding="utf-8")
TREE = ast.parse(SOURCE)
FUNCTIONS = [node for node in ast.walk(TREE) if isinstance(node, ast.FunctionDef)]


def body_lines(function):
    first = function.body[0]
    if isinstance(first, ast.Expr) and isinstance(first.value, ast.Constant):
        first = function.body[1] if len(function.body) > 1 else first
    lines = SOURCE.splitlines()[first.lineno - 1:function.end_lineno]
    return [line for line in lines if line.strip() and not line.strip().startswith("#")]


def test_no_function_is_longer_than_30_lines():
    long = [f.name for f in FUNCTIONS if len(body_lines(f)) > 30]
    assert not long, f"split these functions: {long}"


def test_no_function_calls_itself():
    recursive = [
        f.name for f in FUNCTIONS
        if any(isinstance(n, ast.Call) and isinstance(n.func, ast.Name) and n.func.id == f.name
               for n in ast.walk(f))
    ]
    assert not recursive, f"explore iteratively, not recursively: {recursive}"


def test_no_global_state():
    assert not [n for n in ast.walk(TREE) if isinstance(n, (ast.Global, ast.Nonlocal))]


def test_state_is_still_a_frozen_dataclass():
    assert dataclasses.is_dataclass(State)
    assert State.__dataclass_params__.frozen
