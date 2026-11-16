"""A tiny orchestrator: tasks with dependencies, retries, resumable runs and backfills."""

from collections.abc import Callable, Iterable
from dataclasses import dataclass, field
from datetime import date


@dataclass(frozen=True)
class Context:
    ds: date  # the logical date (partition) the run is for
    attempt: int  # 1 for the first try


@dataclass(frozen=True)
class Task:
    name: str
    fn: Callable[[Context], None]
    upstream: tuple[str, ...] = ()
    retries: int = 0


class QualityGateFailed(Exception):
    """Raised by a task when its data must not go further; never retried."""


@dataclass
class RunState:
    """What already succeeded, so a rerun resumes instead of starting over."""

    done: set[tuple[str, date]] = field(default_factory=set)

    def is_done(self, task: str, ds: date) -> bool:
        return (task, ds) in self.done

    def mark_done(self, task: str, ds: date) -> None:
        self.done.add((task, ds))

    def clear(self, ds: date) -> None:
        """Forget every success of one logical date, so the next run redoes it all."""
        self.done = {(task, day) for task, day in self.done if day != ds}


class DAG:
    def __init__(self, tasks: Iterable[Task]):
        """Keep the tasks by name and check the graph (see the README)."""
        raise NotImplementedError("todo: write DAG.__init__()")

    def order(self) -> list[str]:
        """Every task after its upstream tasks; ties in alphabetical order."""
        raise NotImplementedError("todo: write DAG.order()")


def run(dag: DAG, ds: date, state: RunState) -> dict[str, str]:
    """Run every task for one logical date; returns task -> status, in run order."""
    raise NotImplementedError("todo: write run()")


def backfill(dag: DAG, start: date, end: date, state: RunState) -> dict[date, dict[str, str]]:
    """Run every logical date from start to end (inclusive), oldest first."""
    raise NotImplementedError("todo: write backfill()")
