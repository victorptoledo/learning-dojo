"""An explicit-state model checker for small concurrent programs."""

from collections import deque
from collections.abc import Callable
from dataclasses import dataclass, field

Instr = tuple
Program = list[list[Instr]]

ARITY = {"load": 3, "store": 3, "set": 3, "add": 3, "cas": 5, "lock": 2, "unlock": 2,
         "jump": 2, "jump_if": 5, "critical": 1}
COMPARE = {
    "==": lambda a, b: a == b,
    "!=": lambda a, b: a != b,
    "<": lambda a, b: a < b,
    ">=": lambda a, b: a >= b,
}


class ProgramError(Exception):
    """A thread did something the machine forbids, such as releasing a lock it does not hold."""


class StateSpaceTooLarge(Exception):
    """The exploration reached more states than it was allowed to visit."""


@dataclass(frozen=True)
class State:
    """One global state: every thread's program counter and registers, the shared variables
    and the lock holders. Pairs are kept sorted by name, so equal states are equal tuples."""

    pcs: tuple[int, ...]
    registers: tuple[tuple[tuple[str, int], ...], ...]
    shared: tuple[tuple[str, int], ...]
    holders: tuple[tuple[str, int], ...] = ()

    def pc(self, thread: int) -> int:
        return self.pcs[thread]

    def register(self, thread: int, name: str) -> int:
        return dict(self.registers[thread]).get(name, 0)

    def value(self, variable: str) -> int:
        return dict(self.shared)[variable]

    def holder(self, lock: str) -> int | None:
        return dict(self.holders).get(lock)


@dataclass
class Report:
    states: int
    finals: set[State] = field(default_factory=set)
    violation: list[int] | None = None
    deadlock: list[int] | None = None

    def final_values(self, variable: str) -> set[int]:
        return {state.value(variable) for state in self.finals}


def _pairs(mapping: dict[str, int]) -> tuple[tuple[str, int], ...]:
    return tuple(sorted(mapping.items()))


def validate(threads: Program, shared: dict[str, int]) -> None:
    """Rejects unknown instructions, wrong arities, unknown variables and bad jump targets."""
    for t, code in enumerate(threads):
        for i, instr in enumerate(code):
            where = f"thread {t}, instruction {i}"
            if not instr or instr[0] not in ARITY:
                raise ValueError(f"{where}: unknown instruction {instr!r}")
            if len(instr) != ARITY[instr[0]]:
                raise ValueError(f"{where}: {instr[0]} takes {ARITY[instr[0]] - 1} operands")
            _check_operands(instr, len(code), shared, where)


def _check_operands(instr: Instr, length: int, shared: dict[str, int], where: str) -> None:
    op = instr[0]
    variable = {"load": 2, "store": 1, "cas": 2}.get(op)
    if variable is not None and instr[variable] not in shared:
        raise ValueError(f"{where}: unknown variable {instr[variable]!r}")
    if op == "jump_if" and instr[2] not in COMPARE:
        raise ValueError(f"{where}: unknown comparison {instr[2]!r}")
    target = {"jump": 1, "jump_if": 4}.get(op)
    if target is not None and not 0 <= instr[target] <= length:
        raise ValueError(f"{where}: jump target {instr[target]} is outside the thread")


def initial_state(threads: Program, shared: dict[str, int]) -> State:
    return State(
        pcs=(0,) * len(threads),
        registers=((),) * len(threads),
        shared=_pairs(shared),
    )


def enabled(threads: Program, state: State) -> list[int]:
    """The threads that can take a step: not finished, and not waiting for a lock."""
    ready = []
    for t, code in enumerate(threads):
        pc = state.pc(t)
        if pc == len(code):
            continue
        instr = code[pc]
        if instr[0] == "lock" and state.holder(instr[1]) is not None:
            continue
        ready.append(t)
    return ready


def step(threads: Program, state: State, thread: int) -> State:
    """The state after one atomic step of `thread`. The given state is not changed."""
    instr = threads[thread][state.pc(thread)]
    regs = dict(state.registers[thread])
    shared = dict(state.shared)
    holders = dict(state.holders)
    pc = _execute(instr, thread, regs, shared, holders, state.pc(thread) + 1)
    return State(
        pcs=_replace(state.pcs, thread, pc),
        registers=_replace(state.registers, thread, _pairs(regs)),
        shared=_pairs(shared),
        holders=_pairs(holders),
    )


def _execute(instr: Instr, thread: int, regs: dict, shared: dict, holders: dict, next_pc: int) -> int:
    op = instr[0]
    def value(operand):
        return regs.get(operand, 0) if isinstance(operand, str) else operand
    if op == "load":
        regs[instr[1]] = shared[instr[2]]
    elif op == "store":
        shared[instr[1]] = value(instr[2])
    elif op == "set":
        regs[instr[1]] = value(instr[2])
    elif op == "add":
        regs[instr[1]] = regs.get(instr[1], 0) + value(instr[2])
    elif op == "cas":
        swapped = shared[instr[2]] == value(instr[3])
        if swapped:
            shared[instr[2]] = value(instr[4])
        regs[instr[1]] = int(swapped)
    elif op == "lock":
        holders[instr[1]] = thread
    elif op == "unlock":
        if holders.get(instr[1]) != thread:
            raise ProgramError(f"thread {thread} unlocks {instr[1]}, which it does not hold")
        del holders[instr[1]]
    elif op == "jump":
        return instr[1]
    elif op == "jump_if" and COMPARE[instr[2]](regs.get(instr[1], 0), value(instr[3])):
        return instr[4]
    return next_pc


def _replace(items: tuple, index: int, item) -> tuple:
    return items[:index] + (item,) + items[index + 1:]


def is_final(threads: Program, state: State) -> bool:
    return all(state.pc(t) == len(code) for t, code in enumerate(threads))


def replay(threads: Program, shared: dict[str, int], schedule: list[int]) -> State:
    """Runs a schedule from the initial state; each thread in it must be enabled."""
    state = initial_state(threads, shared)
    for thread in schedule:
        if thread not in enabled(threads, state):
            raise ValueError(f"thread {thread} cannot run now")
        state = step(threads, state, thread)
    return state


def mutual_exclusion(threads: Program) -> Callable[[State], bool]:
    """An invariant: at most one thread is at a `critical` instruction."""
    def holds(state: State) -> bool:
        inside = sum(
            1 for t, code in enumerate(threads)
            if state.pc(t) < len(code) and code[state.pc(t)][0] == "critical"
        )
        return inside <= 1
    return holds


def explore(
    threads: Program,
    shared: dict[str, int],
    invariant: Callable[[State], bool] | None = None,
    max_states: int = 100_000,
) -> Report:
    """Visits every reachable state once, breadth first, so each recorded schedule is a
    shortest one."""
    validate(threads, shared)
    start = initial_state(threads, shared)
    parents: dict[State, tuple[State, int] | None] = {start: None}
    report = Report(states=1)
    _inspect(threads, start, invariant, parents, report)
    queue = deque([start])
    while queue:
        state = queue.popleft()
        for thread in enabled(threads, state):
            try:
                successor = step(threads, state, thread)
            except ProgramError:
                if report.violation is None:
                    report.violation = _schedule(parents, state) + [thread]
                continue
            if successor not in parents:
                _admit(successor, (state, thread), parents, report, max_states)
                _inspect(threads, successor, invariant, parents, report)
                queue.append(successor)
    return report


def _inspect(threads, state, invariant, parents, report) -> None:
    """Checks a state when it is first reached, so the schedules found are shortest."""
    if invariant is not None and report.violation is None and not invariant(state):
        report.violation = _schedule(parents, state)
    if is_final(threads, state):
        report.finals.add(state)
    elif report.deadlock is None and not enabled(threads, state):
        report.deadlock = _schedule(parents, state)


def _admit(state, parent, parents, report, max_states) -> None:
    if report.states >= max_states:
        raise StateSpaceTooLarge(f"more than {max_states} states")
    parents[state] = parent
    report.states += 1


def _schedule(parents: dict, state: State) -> list[int]:
    schedule = []
    while parents[state] is not None:
        state, thread = parents[state]
        schedule.append(thread)
    return schedule[::-1]
