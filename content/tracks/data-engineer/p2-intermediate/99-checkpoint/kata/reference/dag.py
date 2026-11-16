"""A tiny orchestrator: tasks with dependencies, retries, resumable runs and backfills."""

import heapq
from collections.abc import Callable, Iterable
from dataclasses import dataclass, field
from datetime import date, timedelta


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
        self.tasks: dict[str, Task] = {}
        for task in tasks:
            if task.name in self.tasks:
                raise ValueError(f"duplicate task: {task.name}")
            self.tasks[task.name] = task
        for task in self.tasks.values():
            for name in task.upstream:
                if name not in self.tasks:
                    raise ValueError(f"unknown upstream task {name!r} of {task.name!r}")
        self._order = self._topological_order()

    def _topological_order(self) -> list[str]:
        waiting = {name: len(set(task.upstream)) for name, task in self.tasks.items()}
        downstream: dict[str, list[str]] = {name: [] for name in self.tasks}
        for task in self.tasks.values():
            for name in set(task.upstream):
                downstream[name].append(task.name)
        ready = [name for name, count in waiting.items() if count == 0]
        heapq.heapify(ready)
        order = []
        while ready:
            name = heapq.heappop(ready)
            order.append(name)
            for child in downstream[name]:
                waiting[child] -= 1
                if waiting[child] == 0:
                    heapq.heappush(ready, child)
        if len(order) != len(self.tasks):
            stuck = sorted(name for name in self.tasks if name not in order)
            raise ValueError(f"cycle between tasks: {', '.join(stuck)}")
        return order

    def order(self) -> list[str]:
        """Every task after its upstream tasks; ties in alphabetical order."""
        return list(self._order)


OK = ("success", "already_done")


def run(dag: DAG, ds: date, state: RunState) -> dict[str, str]:
    """Run every task for one logical date; returns task -> status, in run order."""
    statuses: dict[str, str] = {}
    for name in dag.order():
        task = dag.tasks[name]
        if any(statuses[up] not in OK for up in task.upstream):
            statuses[name] = "upstream_failed"
        elif state.is_done(name, ds):
            statuses[name] = "already_done"
        else:
            statuses[name] = attempt(task, ds)
            if statuses[name] == "success":
                state.mark_done(name, ds)
    return statuses


def attempt(task: Task, ds: date) -> str:
    for number in range(1, task.retries + 2):
        try:
            task.fn(Context(ds, number))
        except QualityGateFailed:
            return "failed"
        except Exception:
            continue
        return "success"
    return "failed"


def backfill(dag: DAG, start: date, end: date, state: RunState) -> dict[date, dict[str, str]]:
    """Run every logical date from start to end (inclusive), oldest first."""
    if start > end:
        raise ValueError("start is after end")
    results = {}
    day = start
    while day <= end:
        results[day] = run(dag, day, state)
        day += timedelta(days=1)
    return results
