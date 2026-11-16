"""CPU scheduling policies on one core, and the metrics that compare them."""

from collections import deque
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


def _check(jobs: list[Job]) -> None:
    if not jobs:
        raise ValueError("no jobs")
    names = [job.name for job in jobs]
    if len(set(names)) != len(names):
        raise ValueError("job names must be unique")
    for job in jobs:
        if job.arrival < 0 or job.burst <= 0:
            raise ValueError(f"invalid job {job}")


def _add(timeline: list[Slice], name: str, start: int, end: int) -> None:
    """Appends a slice, merging it with the previous one when the same job just continues."""
    if timeline and timeline[-1].name == name and timeline[-1].end == start:
        timeline[-1] = Slice(name, timeline[-1].start, end)
    else:
        timeline.append(Slice(name, start, end))


def fcfs(jobs: list[Job]) -> list[Slice]:
    """First come, first served: arrival order, each job to completion."""
    _check(jobs)
    timeline: list[Slice] = []
    t = 0
    for job in sorted(jobs, key=lambda job: job.arrival):
        t = max(t, job.arrival)
        _add(timeline, job.name, t, t + job.burst)
        t += job.burst
    return timeline


def sjf(jobs: list[Job]) -> list[Slice]:
    """Non-preemptive shortest job first; ties by arrival, then input order."""
    _check(jobs)
    pending = list(range(len(jobs)))
    timeline: list[Slice] = []
    t = 0
    while pending:
        ready = [i for i in pending if jobs[i].arrival <= t]
        if not ready:
            t = min(jobs[i].arrival for i in pending)
            continue
        i = min(ready, key=lambda i: (jobs[i].burst, jobs[i].arrival, i))
        pending.remove(i)
        _add(timeline, jobs[i].name, t, t + jobs[i].burst)
        t += jobs[i].burst
    return timeline


def srtf(jobs: list[Job]) -> list[Slice]:
    """Preemptive shortest remaining time first; ties by arrival, then input order."""
    _check(jobs)
    remaining = [job.burst for job in jobs]
    timeline: list[Slice] = []
    t = 0
    while any(remaining):
        ready = [i for i, left in enumerate(remaining) if left and jobs[i].arrival <= t]
        future = [jobs[i].arrival for i, left in enumerate(remaining) if left and jobs[i].arrival > t]
        if not ready:
            t = min(future)
            continue
        i = min(ready, key=lambda i: (remaining[i], jobs[i].arrival, i))
        until = min([t + remaining[i], *future])
        _add(timeline, jobs[i].name, t, until)
        remaining[i] -= until - t
        t = until
    return timeline


def round_robin(jobs: list[Job], quantum: int, switch_cost: int = 0) -> list[Slice]:
    """Round robin with a time quantum and a cost for switching between different jobs."""
    _check(jobs)
    if quantum <= 0 or switch_cost < 0:
        raise ValueError("quantum must be positive and switch_cost not negative")
    order = sorted(range(len(jobs)), key=lambda i: jobs[i].arrival)
    remaining = [job.burst for job in jobs]
    queue: deque[int] = deque()
    timeline: list[Slice] = []
    admitted = 0
    t = 0
    last: int | None = None

    def admit(upto: int) -> None:
        nonlocal admitted
        while admitted < len(order) and jobs[order[admitted]].arrival <= upto:
            queue.append(order[admitted])
            admitted += 1

    admit(t)
    while queue or admitted < len(order):
        if not queue:
            t = jobs[order[admitted]].arrival
            last = None
            admit(t)
            continue
        i = queue.popleft()
        if last is not None and last != i:
            t += switch_cost
            admit(t)
        run = min(quantum, remaining[i])
        _add(timeline, jobs[i].name, t, t + run)
        t += run
        remaining[i] -= run
        admit(t)
        if remaining[i]:
            queue.append(i)
        last = i
    return timeline


def metrics(jobs: list[Job], timeline: list[Slice]) -> Metrics:
    """Turnaround, waiting and response time of every job in a timeline."""
    turnaround: dict[str, int] = {}
    waiting: dict[str, int] = {}
    response: dict[str, int] = {}
    for job in jobs:
        slices = [s for s in timeline if s.name == job.name]
        if sum(s.end - s.start for s in slices) != job.burst:
            raise ValueError(f"the timeline does not run {job.name} for its whole burst")
        finish = max(s.end for s in slices)
        turnaround[job.name] = finish - job.arrival
        waiting[job.name] = turnaround[job.name] - job.burst
        response[job.name] = min(s.start for s in slices) - job.arrival
    return Metrics(turnaround, waiting, response)
