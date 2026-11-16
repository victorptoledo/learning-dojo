# Orchestration: DAGs, Retries and Backfills

A pipeline is many steps: extract three sources, load them, check them, build the models,
refresh the dashboard. A `cron` line per script runs them on time, but it cannot express
"build the model only after both extracts succeeded", retry a flaky API, rerun last
Tuesday, or show you what failed. An **orchestrator** does.

## DAGs

A pipeline is a **directed acyclic graph** of **tasks**: arrows are dependencies, and there
are no cycles, so there is always an order in which every task runs after its upstream
tasks (a **topological order**).

```text
extract_orders ----                        >-- check_orders -- load_orders -- daily_revenue
load_customers ----/
```

The orchestrator runs ready tasks (all upstream done), in parallel where it can, and
records each task's state: queued, running, success, failed, **upstream failed** (skipped
because something it needs failed), up for retry.

## Logical dates

A daily run that starts at 01:00 on 16 November processes the data **of 15 November**.
Orchestrators make that explicit: each run has a **logical date** (Airflow's `ds`, or a
data interval `[2026-11-15, 2026-11-16)`), and tasks receive it as a parameter.

The rule that makes everything else work: **a task's output depends only on its logical
date and its inputs, never on when it actually runs**. `WHERE day = :ds`, not
`WHERE day = current_date - 1`. Then rerunning 15 November next month gives the same result,
and a backfill is just "run these dates".

## Retries, timeouts and idempotency

- **Retries** with a delay (often exponential backoff) absorb transient failures: a timeout,
  a rate limit, a database failover.
- **Timeouts** stop a hung task from blocking everything behind it.
- Retries are only safe if tasks are **idempotent**: a task that failed halfway, or
  succeeded but timed out before reporting, will run again. Overwrite the partition of the
  logical date, upsert, or write to a temporary location and swap.
- Do **not** retry failures that will not fix themselves: a quality gate failure, invalid
  credentials, a syntax error. Fail fast and alert.

## Scheduling

- **Time-based**: run daily at 01:00. Simple, but if the upstream file arrives late, the run
  processes nothing.
- **Sensors**: a task that waits until a condition holds (a file exists, a partition is
  ready), with a timeout.
- **Data-aware** (event or asset-based): run when the upstream dataset is updated (Airflow
  Datasets and Assets, Dagster software-defined assets). Pipelines become a graph of
  **data assets** rather than of jobs.

## Backfills and catch-up

A **backfill** runs the DAG for past logical dates: after a bug fix, for a new table, or
after an outage. Orchestrators can do it from the command line or UI; **catch-up** does it
automatically for missed intervals when a DAG is resumed. Plan backfills:

- run in **date order** when a day depends on the previous one (running totals, type 2
  history), otherwise in parallel with a **limit** that protects the warehouse and sources;
- backfill **downstream** tasks too, in dependency order;
- compare row counts and totals before and after, and tell consumers.

## The tools

| Tool | Model | Notes |
| --- | --- | --- |
| **Apache Airflow** | DAGs of tasks in Python; operators for every system | the most widely used; Airflow 3 adds assets and a new UI |
| **Dagster** | software-defined **assets** with lineage and checks | strong local development and testing |
| **Prefect** | Python flows and tasks, little ceremony | easy to start, dynamic workflows |
| Managed services | AWS Step Functions, Azure Data Factory, Google Cloud Composer (managed Airflow) | less to operate |

Keep business logic **out of** the orchestrator: tasks should call code (SQL models, Python
modules) that you can run and test without it.

## Summary

- A DAG states dependencies; the orchestrator runs tasks in topological order and tracks state.
- Every run has a logical date; tasks depend on it, never on the clock.
- Retry transient failures, time out hung tasks, never retry a quality gate.
- Idempotent, date-keyed tasks make reruns and backfills safe.
- Prefer data-aware scheduling when upstream timing varies.

## Further reading

- Apache Airflow documentation, core concepts (DAGs, tasks, DAG runs and data intervals): <https://airflow.apache.org/docs/apache-airflow/stable/core-concepts/index.html>
- Apache Airflow documentation, best practices: <https://airflow.apache.org/docs/apache-airflow/stable/best-practices.html>
- Dagster documentation, assets: <https://docs.dagster.io/guides/build/assets>
- Prefect documentation, flows and tasks: <https://docs.prefect.io/>
- Maxime Beauchemin, "Functional Data Engineering": <https://maximebeauchemin.medium.com/functional-data-engineering-a-modern-paradigm-for-batch-data-processing-2327ec32c42a>
