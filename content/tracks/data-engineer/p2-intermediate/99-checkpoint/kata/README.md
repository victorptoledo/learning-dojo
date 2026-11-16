# Checkpoint 2: A Daily Pipeline on Your Own Orchestrator

Orchestrators such as Airflow, Dagster and Prefect share a few ideas: a pipeline is a
**DAG** of tasks, each run is for a **logical date** (`ds`), failed tasks are retried, a
rerun resumes where the last one stopped, and a **backfill** runs a range of past dates.
Build a tiny one in `dag.py`, then use it for a daily orders pipeline in `pipeline.py`.
`sources.py` (the source API and the warehouse) is given; do not change it.

## `dag.py`

`Context`, `Task`, `QualityGateFailed` and `RunState` are written. Write the rest:

**`DAG(tasks)`** keeps the tasks in `dag.tasks` (a `dict` name -> `Task`; `tasks` may be a
generator) and refuses a bad graph with `ValueError`: a duplicate name (message contains
`duplicate`), an upstream name that is not a task (`unknown`), or a cycle, including a task
that depends on itself (`cycle`). **`dag.order()`** lists every task after all its upstream
tasks; when several are ready, the alphabetically first goes first.

**`run(dag, ds, state)`** runs the tasks in that order for one logical date and returns
`{task name: status}` in the same order:

| Status | When |
| --- | --- |
| `upstream_failed` | an upstream task did not end `success` or `already_done`; the task is not called |
| `already_done` | `state` says it succeeded for this `ds` before; it is not called again |
| `success` | it returned; record it in `state` |
| `failed` | it raised on every attempt: 1 + `retries` attempts, with `Context(ds, attempt)` numbered from 1. `QualityGateFailed` is **never retried**: it fails at once |

Other branches of the DAG still run when one task fails.

**`backfill(dag, start, end, state)`** runs every date from `start` to `end` inclusive,
oldest first, and returns `{ds: statuses}`. A failed day does not stop it; `start` after
`end` is a `ValueError`.

## `pipeline.py`

`build_dag(source, warehouse)` returns a DAG with five tasks. Each one must be
**idempotent**: running it twice for the same `ds` leaves the same warehouse.

| Task | Upstream | Retries | Does |
| --- | --- | --- | --- |
| `load_customers` | - | 1 | upserts every `source.customers()` row into `warehouse.dim_customer[customer_id]` |
| `extract_orders` | - | 2 | `warehouse.staging[ds] = source.orders_for(ds)` (the API may raise `ConnectionError`) |
| `check_orders` | `extract_orders`, `load_customers` | 0 | the **quality gate**: raises `QualityGateFailed` if the day's staged orders have a duplicate `order_id`, a negative `amount_cents` or a `customer_id` not in `dim_customer` |
| `load_orders` | `check_orders` | 0 | **overwrites** the partition `warehouse.fact_orders[ds]` with the staged orders |
| `daily_revenue` | `load_orders` | 0 | `warehouse.daily_revenue[ds]` = the sum of `amount_cents` of that partition (0 for a day without orders) |

Because every write replaces one partition, a day can be rerun from scratch
(`state.clear(ds)`, then `run`) after the source corrects its data, and a backfill can run
twice without double counting.

> [!TIP]
> Copy the rows you store (`dict(row)`), so a later change in the source cannot reach into
> the warehouse.

Run the visible tests with `uv run pytest`. There are 33 tests: 2 in `tests/test_visible.py`
and 28 hidden ones, plus ruff on `dag.py`, `pipeline.py` and `sources.py`.
