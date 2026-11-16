from datetime import date

from dag import DAG, RunState, Task, run
from pipeline import build_dag
from sources import Warehouse


def noop(ctx):
    pass


def test_tasks_run_after_their_upstream_tasks():
    dag = DAG([Task("load", noop, upstream=("extract",)), Task("extract", noop)])
    assert dag.order() == ["extract", "load"]
    assert run(dag, date(2026, 11, 1), RunState()) == {"extract": "success", "load": "success"}


class Shop:
    def customers(self):
        return [{"customer_id": 1, "name": "Ada", "country": "GB"}]

    def orders_for(self, ds):
        return [{"order_id": 10, "customer_id": 1, "amount_cents": 1990}]


def test_one_day_of_the_pipeline():
    warehouse = Warehouse()
    run(build_dag(Shop(), warehouse), date(2026, 11, 1), RunState())
    assert warehouse.daily_revenue == {date(2026, 11, 1): 1990}
