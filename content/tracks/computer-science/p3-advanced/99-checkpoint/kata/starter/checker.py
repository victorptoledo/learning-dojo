"""An explicit-state model checker for small concurrent programs."""

from collections.abc import Callable
from dataclasses import dataclass, field

Instr = tuple
Program = list[list[Instr]]


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


def validate(threads: Program, shared: dict[str, int]) -> None:
    """Rejects unknown instructions, wrong arities, unknown variables and bad jump targets."""
    raise NotImplementedError("todo: write validate()")


def initial_state(threads: Program, shared: dict[str, int]) -> State:
    raise NotImplementedError("todo: write initial_state()")


def enabled(threads: Program, state: State) -> list[int]:
    """The threads that can take a step: not finished, and not waiting for a lock."""
    raise NotImplementedError("todo: write enabled()")


def step(threads: Program, state: State, thread: int) -> State:
    """The state after one atomic step of `thread`. The given state is not changed."""
    raise NotImplementedError("todo: write step()")


def is_final(threads: Program, state: State) -> bool:
    raise NotImplementedError("todo: write is_final()")


def replay(threads: Program, shared: dict[str, int], schedule: list[int]) -> State:
    """Runs a schedule from the initial state; each thread in it must be enabled."""
    raise NotImplementedError("todo: write replay()")


def mutual_exclusion(threads: Program) -> Callable[[State], bool]:
    """An invariant: at most one thread is at a `critical` instruction."""
    raise NotImplementedError("todo: write mutual_exclusion()")


def explore(
    threads: Program,
    shared: dict[str, int],
    invariant: Callable[[State], bool] | None = None,
    max_states: int = 100_000,
) -> Report:
    """Visits every reachable state once and reports shortest schedules to problems."""
    raise NotImplementedError("todo: write explore()")
