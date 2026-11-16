# CPU Scheduler

Simulate four scheduling policies on one CPU in `scheduler.py`, then measure them. The dataclasses
`Job(name, arrival, burst)`, `Slice(name, start, end)` and `Metrics` are already written.

## Policies

Each policy takes a list of jobs and returns the **timeline**: the slices during which each job
ran, in time order. Times are integers.

| Function | Policy |
| --- | --- |
| `fcfs(jobs)` | first come, first served: by arrival, each job runs to completion |
| `sjf(jobs)` | non-preemptive shortest job first: whenever the CPU is free, the ready job with the smallest burst runs to completion |
| `srtf(jobs)` | preemptive shortest remaining time first: at every moment the ready job with the least remaining time runs, so a shorter job that arrives preempts the running one |
| `round_robin(jobs, quantum, switch_cost=0)` | the ready queue in FIFO order, each job runs for at most `quantum`, then goes to the back of the queue |

Rules that make the timeline unique:

- **Ties**: equal arrival times keep the order of the input list. In `sjf` and `srtf`, equal
  bursts (or remaining times) go to the earlier arrival, then to the input order; a running job is
  not preempted by one with an *equal* remaining time.
- **Idle CPU**: when no job is ready, the CPU waits for the next arrival; no slice is recorded.
- **Merging**: when the same job keeps the CPU (it is alone, or chosen again), its slices are
  merged into one: `round_robin([Job("solo", 0, 7)], 3) == [Slice("solo", 0, 7)]`.
- **Round robin arrivals**: a job that arrives at the moment a quantum ends joins the queue
  **before** the preempted job.
- **Switch cost**: in `round_robin`, starting a job that is different from the one that just ran
  first costs `switch_cost` time units, during which the CPU does no work (a gap between slices).
  There is no switch at the very start or after the CPU was idle. The next job is chosen before
  the switch; jobs that arrive during it join the queue behind it.
- **Invalid input**: an empty list, duplicate names, a negative arrival, a burst ≤ 0, a quantum
  ≤ 0 or a negative switch cost raise `ValueError`.

```python
jobs = [Job("A", 0, 8), Job("B", 1, 4), Job("C", 2, 9), Job("D", 3, 5)]
round_robin(jobs, 3)
# A 0–3, B 3–6, C 6–9, D 9–12, A 12–15, B 15–16, C 16–19, D 19–21, A 21–23, C 23–26
```

## Metrics

`metrics(jobs, timeline)` returns a `Metrics` with three dicts keyed by job name:

- `turnaround` = completion − arrival
- `waiting` = turnaround − burst
- `response` = first start − arrival

It raises `ValueError` if the timeline does not run some job for exactly its burst. The averages
are properties of `Metrics` already.

## What the hidden tests check

The example under every policy, the tie, merge, arrival and switch rules, idle gaps, invalid
input, and hundreds of random workloads: every timeline must be valid, `srtf` must never have a
higher average waiting time than any other policy (it is optimal), and `sjf` must beat `fcfs` when
all jobs arrive together.

Run the visible tests with `uv run pytest`.
