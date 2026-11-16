"""CPU scheduling policies on one core, and the metrics that compare them."""

from collections import deque  # noqa: F401  (a good queue for round robin)
from dataclasses import dataclass


@dataclass(frozen=True)
class Job:
    name: str
    arrival: int
    burst: int


@dataclass(frozen=True)
class Slice:
    name: str
    start: int
    end: int


@dataclass(frozen=True)
class Metrics:
    turnaround: dict[str, int]
    waiting: dict[str, int]
    response: dict[str, int]

    @property
    def average_turnaround(self) -> float:
        return sum(self.turnaround.values()) / len(self.turnaround)

    @property
    def average_waiting(self) -> float:
        return sum(self.waiting.values()) / len(self.waiting)

    @property
    def average_response(self) -> float:
        return sum(self.response.values()) / len(self.response)


def fcfs(jobs: list[Job]) -> list[Slice]:
    """First come, first served: arrival order, each job to completion."""
    raise NotImplementedError("todo: write fcfs()")


def sjf(jobs: list[Job]) -> list[Slice]:
    """Non-preemptive shortest job first; ties by arrival, then input order."""
    raise NotImplementedError("todo: write sjf()")


def srtf(jobs: list[Job]) -> list[Slice]:
    """Preemptive shortest remaining time first; ties by arrival, then input order."""
    raise NotImplementedError("todo: write srtf()")


def round_robin(jobs: list[Job], quantum: int, switch_cost: int = 0) -> list[Slice]:
    """Round robin with a time quantum and a cost for switching between different jobs."""
    raise NotImplementedError("todo: write round_robin()")


def metrics(jobs: list[Job], timeline: list[Slice]) -> Metrics:
    """Turnaround, waiting and response time of every job in a timeline."""
    raise NotImplementedError("todo: write metrics()")
