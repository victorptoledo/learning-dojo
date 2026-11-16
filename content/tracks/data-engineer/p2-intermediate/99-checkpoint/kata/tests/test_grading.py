from datetime import date

import pytest

from dag import DAG, Context, QualityGateFailed, RunState, Task, backfill, run
from pipeline import build_dag
from sources import Warehouse

DAY = date(2026, 11, 1)


class Recorder:
    """Task functions that record their calls and fail as told."""

    def __init__(self):
        self.calls = []

    def task(self, name, fails=0, error=RuntimeError, **kwargs):
        remaining = {"n": fails}

        def fn(ctx):
            self.calls.append((name, ctx.ds, ctx.attempt))
            if remaining["n"]:
                remaining["n"] -= 1
                raise error(f"{name} failed")

        return Task(name, fn, **kwargs)


def noop(ctx):
    pass


# ---- the graph ------------------------------------------------------------------------

def test_order_breaks_ties_alphabetically():
    dag = DAG([
        Task("report", noop, upstream=("load_b", "load_a")),
        Task("load_b", noop, upstream=("extract",)),
        Task("load_a", noop, upstream=("extract",)),
        Task("extract", noop),
        Task("audit", noop),
    ])
    assert dag.order() == ["audit", "extract", "load_a", "load_b", "report"]


def test_order_is_not_alphabetical_when_dependencies_say_otherwise():
    dag = DAG([Task("a_load", noop, upstream=("z_extract",)), Task("z_extract", noop),
               Task("m_other", noop)])
    assert dag.order() == ["m_other", "z_extract", "a_load"]


def test_duplicate_task_names_are_refused():
    with pytest.raises(ValueError, match="duplicate"):
        DAG([Task("a", noop), Task("a", noop)])


def test_unknown_upstream_tasks_are_refused():
    with pytest.raises(ValueError, match="unknown"):
        DAG([Task("load", noop, upstream=("extract",))])


def test_cycles_are_refused():
    with pytest.raises(ValueError, match="cycle"):
        DAG([Task("a", noop, upstream=("c",)), Task("b", noop, upstream=("a",)),
             Task("c", noop, upstream=("b",)), Task("d", noop)])
    with pytest.raises(ValueError, match="cycle"):
        DAG([Task("self", noop, upstream=("self",))])


def test_a_dag_accepts_a_generator_of_tasks():
    dag = DAG(Task(name, noop) for name in ["b", "a"])
    assert dag.order() == ["a", "b"]


# ---- one run ----------------------------------------------------------------------------

def test_tasks_get_the_logical_date_and_the_attempt():
    rec = Recorder()
    run(DAG([rec.task("extract")]), DAY, RunState())
    assert rec.calls == [("extract", DAY, 1)]


def test_a_failing_task_is_retried():
    rec = Recorder()
    statuses = run(DAG([rec.task("extract", fails=2, retries=2)]), DAY, RunState())
    assert statuses == {"extract": "success"}
    assert rec.calls == [("extract", DAY, 1), ("extract", DAY, 2), ("extract", DAY, 3)]


def test_a_task_fails_when_its_retries_run_out():
    rec = Recorder()
    statuses = run(DAG([rec.task("extract", fails=3, retries=1)]), DAY, RunState())
    assert statuses == {"extract": "failed"}
    assert len(rec.calls) == 2


def test_a_quality_gate_is_never_retried():
    rec = Recorder()
    statuses = run(DAG([rec.task("check", fails=1, error=QualityGateFailed, retries=3)]),
                   DAY, RunState())
    assert statuses == {"check": "failed"}
    assert len(rec.calls) == 1


def test_downstream_of_a_failure_is_skipped_and_other_branches_run():
    rec = Recorder()
    dag = DAG([
        rec.task("extract_a", fails=1),
        rec.task("load_a", upstream=("extract_a",)),
        rec.task("report", upstream=("load_a",)),
        rec.task("extract_b"),
        rec.task("load_b", upstream=("extract_b",)),
    ])
    statuses = run(dag, DAY, RunState())
    assert statuses == {"extract_a": "failed", "extract_b": "success",
                        "load_a": "upstream_failed", "load_b": "success",
                        "report": "upstream_failed"}
    assert [name for name, _, _ in rec.calls] == ["extract_a", "extract_b", "load_b"]


def test_statuses_come_in_run_order():
    dag = DAG([Task("b", noop, upstream=("a",)), Task("a", noop)])
    assert list(run(dag, DAY, RunState())) == ["a", "b"]


def test_a_rerun_resumes_after_the_last_success():
    rec = Recorder()
    dag = DAG([rec.task("extract"), rec.task("load", fails=1, upstream=("extract",)),
               rec.task("report", upstream=("load",))])
    state = RunState()
    assert run(dag, DAY, state) == {"extract": "success", "load": "failed",
                                    "report": "upstream_failed"}
    assert run(dag, DAY, state) == {"extract": "already_done", "load": "success",
                                    "report": "success"}
    assert [name for name, _, _ in rec.calls] == ["extract", "load", "load", "report"]


def test_successes_are_kept_per_logical_date():
    rec = Recorder()
    dag = DAG([rec.task("extract")])
    state = RunState()
    run(dag, DAY, state)
    assert run(dag, date(2026, 11, 2), state) == {"extract": "success"}
    assert run(dag, DAY, state) == {"extract": "already_done"}


# ---- backfill -------------------------------------------------------------------------

def test_backfill_runs_every_day_oldest_first():
    rec = Recorder()
    results = backfill(DAG([rec.task("extract")]), date(2026, 10, 30), date(2026, 11, 2),
                       RunState())
    days = [date(2026, 10, 30), date(2026, 10, 31), date(2026, 11, 1), date(2026, 11, 2)]
    assert list(results) == days
    assert [ds for _, ds, _ in rec.calls] == days


def test_backfill_of_one_day_and_of_a_reversed_range():
    assert list(backfill(DAG([Task("a", noop)]), DAY, DAY, RunState())) == [DAY]
    with pytest.raises(ValueError):
        backfill(DAG([Task("a", noop)]), date(2026, 11, 2), DAY, RunState())


def test_a_failed_day_does_not_stop_the_backfill():
    calls = []

    def extract(ctx):
        calls.append(ctx.ds)
        if ctx.ds == date(2026, 11, 2):
            raise ConnectionError("down")

    results = backfill(DAG([Task("extract", extract)]), DAY, date(2026, 11, 3), RunState())
    assert [r["extract"] for r in results.values()] == ["success", "failed", "success"]


# ---- the pipeline -----------------------------------------------------------------------

class Shop:
    def __init__(self, orders, flaky=0):
        self.orders = orders
        self.flaky = flaky
        self.order_calls = 0
        self.people = [{"customer_id": 1, "name": "Ada", "country": "GB"},
                       {"customer_id": 2, "name": "Grace", "country": "US"}]

    def customers(self):
        return [dict(c) for c in self.people]

    def orders_for(self, ds):
        self.order_calls += 1
        if self.flaky:
            self.flaky -= 1
            raise ConnectionError("the API timed out")
        return [dict(o) for o in self.orders.get(ds, [])]


def order(order_id, customer_id=1, amount_cents=1000):
    return {"order_id": order_id, "customer_id": customer_id, "amount_cents": amount_cents}


NOV = {
    date(2026, 11, 1): [order(1), order(2, 2, 500)],
    date(2026, 11, 2): [order(3, 2, 250)],
    date(2026, 11, 3): [],
}


def test_the_pipeline_wires_five_tasks():
    dag = build_dag(Shop(NOV), Warehouse())
    assert dag.order() == ["extract_orders", "load_customers", "check_orders", "load_orders",
                           "daily_revenue"]


def test_the_pipeline_loads_a_day():
    warehouse = Warehouse()
    statuses = run(build_dag(Shop(NOV), warehouse), DAY, RunState())
    assert set(statuses.values()) == {"success"}
    assert sorted(warehouse.dim_customer) == [1, 2]
    assert warehouse.dim_customer[2] == {"customer_id": 2, "name": "Grace", "country": "US"}
    assert warehouse.fact_orders == {DAY: NOV[DAY]}
    assert warehouse.daily_revenue == {DAY: 1500}


def test_the_extract_survives_two_timeouts():
    shop = Shop(NOV, flaky=2)
    warehouse = Warehouse()
    statuses = run(build_dag(shop, warehouse), DAY, RunState())
    assert statuses["extract_orders"] == "success"
    assert shop.order_calls == 3
    assert warehouse.daily_revenue == {DAY: 1500}


def test_the_extract_gives_up_after_three_timeouts():
    warehouse = Warehouse()
    statuses = run(build_dag(Shop(NOV, flaky=3), warehouse), DAY, RunState())
    assert statuses["extract_orders"] == "failed"
    assert statuses["daily_revenue"] == "upstream_failed"
    assert warehouse.fact_orders == {}


@pytest.mark.parametrize(
    "bad_orders",
    [
        [order(1), order(1)],
        [order(1, amount_cents=-1)],
        [order(1, customer_id=99)],
    ],
)
def test_the_quality_gate_stops_bad_orders(bad_orders):
    warehouse = Warehouse()
    shop = Shop({DAY: bad_orders})
    statuses = run(build_dag(shop, warehouse), DAY, RunState())
    assert statuses["check_orders"] == "failed"
    assert statuses["load_orders"] == "upstream_failed"
    assert DAY not in warehouse.fact_orders
    assert DAY not in warehouse.daily_revenue
    assert shop.order_calls == 1


def test_the_quality_gate_raises_quality_gate_failed():
    warehouse = Warehouse()
    shop = Shop({DAY: [order(1, customer_id=99)]})
    dag = build_dag(shop, warehouse)
    run(dag, DAY, RunState())
    with pytest.raises(QualityGateFailed):
        dag.tasks["check_orders"].fn(Context(DAY, 1))


def test_a_backfill_fills_every_partition():
    warehouse = Warehouse()
    backfill(build_dag(Shop(NOV), warehouse), DAY, date(2026, 11, 3), RunState())
    assert warehouse.daily_revenue == {DAY: 1500, date(2026, 11, 2): 250, date(2026, 11, 3): 0}
    assert warehouse.fact_orders[date(2026, 11, 3)] == []


def test_running_a_day_again_from_scratch_overwrites_its_partition():
    shop = Shop(NOV)
    warehouse = Warehouse()
    dag = build_dag(shop, warehouse)
    state = RunState()
    backfill(dag, DAY, date(2026, 11, 3), state)
    shop.orders = {**NOV, DAY: [order(1, amount_cents=900)]}  # the source corrected day 1
    state.clear(DAY)
    run(dag, DAY, state)
    run(dag, DAY, RunState())  # and once more, as if the state was lost
    assert warehouse.fact_orders[DAY] == [order(1, amount_cents=900)]
    assert warehouse.daily_revenue == {DAY: 900, date(2026, 11, 2): 250, date(2026, 11, 3): 0}


def test_a_fixed_source_passes_the_gate_after_a_clear():
    shop = Shop({DAY: [order(1), order(1)]})
    warehouse = Warehouse()
    dag = build_dag(shop, warehouse)
    state = RunState()
    run(dag, DAY, state)
    shop.orders = {DAY: [order(1)]}
    state.clear(DAY)
    assert run(dag, DAY, state)["daily_revenue"] == "success"
    assert warehouse.daily_revenue == {DAY: 1000}
