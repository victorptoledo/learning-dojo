from scheduler import Job, Slice, fcfs, metrics

JOBS = [Job("A", 0, 8), Job("B", 1, 4), Job("C", 2, 9), Job("D", 3, 5)]


def test_fcfs_runs_jobs_in_arrival_order():
    assert fcfs(JOBS) == [Slice("A", 0, 8), Slice("B", 8, 12), Slice("C", 12, 21), Slice("D", 21, 26)]


def test_fcfs_metrics():
    result = metrics(JOBS, fcfs(JOBS))
    assert result.turnaround == {"A": 8, "B": 11, "C": 19, "D": 23}
    assert result.average_waiting == 8.75
