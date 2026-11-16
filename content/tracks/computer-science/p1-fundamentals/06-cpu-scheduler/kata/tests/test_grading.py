import random

import pytest

from scheduler import Job, Slice, fcfs, metrics, round_robin, sjf, srtf

JOBS = [Job("A", 0, 8), Job("B", 1, 4), Job("C", 2, 9), Job("D", 3, 5)]


def S(name, start, end):
    return Slice(name, start, end)


def test_fcfs_of_the_example():
    assert fcfs(JOBS) == [S("A", 0, 8), S("B", 8, 12), S("C", 12, 21), S("D", 21, 26)]


def test_fcfs_sorts_by_arrival_and_keeps_input_order_on_ties():
    jobs = [Job("late", 5, 1), Job("x", 0, 2), Job("y", 0, 2)]
    assert fcfs(jobs) == [S("x", 0, 2), S("y", 2, 4), S("late", 5, 6)]


def test_sjf_of_the_example():
    assert sjf(JOBS) == [S("A", 0, 8), S("B", 8, 12), S("D", 12, 17), S("C", 17, 26)]


def test_sjf_breaks_ties_by_arrival_then_input_order():
    jobs = [Job("long", 0, 10), Job("p", 3, 2), Job("q", 1, 2), Job("r", 1, 2)]
    assert sjf(jobs) == [S("long", 0, 10), S("q", 10, 12), S("r", 12, 14), S("p", 14, 16)]


def test_srtf_preempts_when_a_shorter_job_arrives():
    assert srtf(JOBS) == [
        S("A", 0, 1), S("B", 1, 5), S("D", 5, 10), S("A", 10, 17), S("C", 17, 26),
    ]


def test_srtf_does_not_preempt_for_an_equal_remaining_time():
    jobs = [Job("a", 0, 4), Job("b", 2, 2)]
    assert srtf(jobs) == [S("a", 0, 4), S("b", 4, 6)]


def test_round_robin_of_the_example():
    assert round_robin(JOBS, 3) == [
        S("A", 0, 3), S("B", 3, 6), S("C", 6, 9), S("D", 9, 12), S("A", 12, 15),
        S("B", 15, 16), S("C", 16, 19), S("D", 19, 21), S("A", 21, 23), S("C", 23, 26),
    ]


def test_round_robin_new_arrivals_queue_before_the_preempted_job():
    jobs = [Job("a", 0, 4), Job("b", 2, 2)]
    # b arrives exactly when a's quantum ends, so it runs before a continues
    assert round_robin(jobs, 2) == [S("a", 0, 2), S("b", 2, 4), S("a", 4, 6)]


def test_round_robin_merges_a_job_that_keeps_the_cpu():
    assert round_robin([Job("solo", 0, 7)], 3) == [S("solo", 0, 7)]
    assert round_robin([Job("a", 0, 2), Job("b", 0, 6)], 2) == [S("a", 0, 2), S("b", 2, 8)]


def test_round_robin_charges_switches_between_different_jobs():
    jobs = [Job("A", 0, 5), Job("B", 0, 3)]
    assert round_robin(jobs, 2, switch_cost=1) == [
        S("A", 0, 2), S("B", 3, 5), S("A", 6, 8), S("B", 9, 10), S("A", 11, 12),
    ]


def test_round_robin_no_switch_cost_after_idle_time():
    jobs = [Job("a", 0, 2), Job("b", 10, 3)]
    assert round_robin(jobs, 5, switch_cost=2) == [S("a", 0, 2), S("b", 10, 13)]


def test_round_robin_arrivals_during_a_switch_join_the_queue():
    jobs = [Job("a", 0, 2), Job("b", 0, 2), Job("c", 3, 1)]
    # a 0-2, switch 2-3 (c arrives at 3, after b was chosen), b 3-5, switch, c 6-7
    assert round_robin(jobs, 2, switch_cost=1) == [S("a", 0, 2), S("b", 3, 5), S("c", 6, 7)]


def test_every_policy_handles_idle_gaps():
    jobs = [Job("a", 2, 1), Job("b", 9, 2)]
    expected = [S("a", 2, 3), S("b", 9, 11)]
    assert fcfs(jobs) == sjf(jobs) == srtf(jobs) == round_robin(jobs, 4) == expected


def test_metrics_of_the_example():
    m = metrics(JOBS, round_robin(JOBS, 3))
    assert m.turnaround == {"A": 23, "B": 15, "C": 24, "D": 18}
    assert m.waiting == {"A": 15, "B": 11, "C": 15, "D": 13}
    assert m.response == {"A": 0, "B": 2, "C": 4, "D": 6}
    assert (m.average_turnaround, m.average_waiting, m.average_response) == (20, 13.5, 3)
    s = metrics(JOBS, sjf(JOBS))
    assert (s.average_turnaround, s.average_waiting) == (14.25, 7.75)


def test_metrics_rejects_a_timeline_that_does_not_finish_a_job():
    with pytest.raises(ValueError):
        metrics(JOBS, [S("A", 0, 8)])


@pytest.mark.parametrize(
    "jobs",
    [[], [Job("a", 0, 0)], [Job("a", -1, 2)], [Job("a", 0, 1), Job("a", 1, 1)]],
)
def test_invalid_jobs_are_rejected(jobs):
    for policy in (fcfs, sjf, srtf, lambda j: round_robin(j, 2)):
        with pytest.raises(ValueError):
            policy(jobs)


def test_invalid_round_robin_parameters_are_rejected():
    for quantum, cost in [(0, 0), (-1, 0), (2, -1)]:
        with pytest.raises(ValueError):
            round_robin(JOBS, quantum, cost)


def _random_jobs(rng):
    return [Job(f"j{i}", rng.randint(0, 30), rng.randint(1, 12)) for i in range(rng.randint(1, 9))]


def _check_valid(jobs, timeline):
    arrival = {job.name: job.arrival for job in jobs}
    for before, after in zip(timeline, timeline[1:]):
        assert before.end <= after.start, f"overlap: {before} {after}"
        assert not (before.name == after.name and before.end == after.start), "unmerged slices"
    for s in timeline:
        assert s.start < s.end and s.start >= arrival[s.name]
    metrics(jobs, timeline)  # every job ran exactly its burst


def test_random_workloads_give_valid_timelines():
    rng = random.Random(5)
    for _ in range(300):
        jobs = _random_jobs(rng)
        for timeline in (fcfs(jobs), sjf(jobs), srtf(jobs),
                         round_robin(jobs, rng.randint(1, 6), rng.randint(0, 2))):
            _check_valid(jobs, timeline)


def test_srtf_has_the_lowest_average_waiting_time():
    rng = random.Random(6)
    for _ in range(300):
        jobs = _random_jobs(rng)
        best = metrics(jobs, srtf(jobs)).average_waiting
        for timeline in (fcfs(jobs), sjf(jobs), round_robin(jobs, 2)):
            assert best <= metrics(jobs, timeline).average_waiting + 1e-9


def test_sjf_beats_fcfs_when_everyone_arrives_together():
    rng = random.Random(9)
    for _ in range(200):
        jobs = [Job(f"j{i}", 0, rng.randint(1, 20)) for i in range(rng.randint(2, 8))]
        assert metrics(jobs, sjf(jobs)).average_waiting <= metrics(jobs, fcfs(jobs)).average_waiting
