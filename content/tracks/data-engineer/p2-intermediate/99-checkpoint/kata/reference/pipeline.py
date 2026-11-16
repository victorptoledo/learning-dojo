"""The daily orders pipeline, as a DAG of idempotent tasks."""

from collections import Counter

from dag import DAG, Context, QualityGateFailed, Task
from sources import Source, Warehouse


def build_dag(source: Source, warehouse: Warehouse) -> DAG:
    def load_customers(ctx: Context) -> None:
        for customer in source.customers():
            warehouse.dim_customer[customer["customer_id"]] = dict(customer)

    def extract_orders(ctx: Context) -> None:
        warehouse.staging[ctx.ds] = [dict(order) for order in source.orders_for(ctx.ds)]

    def check_orders(ctx: Context) -> None:
        orders = warehouse.staging[ctx.ds]
        problems = []
        counts = Counter(order["order_id"] for order in orders)
        duplicated = sorted(order_id for order_id, n in counts.items() if n > 1)
        if duplicated:
            problems.append(f"duplicate order_id {duplicated}")
        if any(order["amount_cents"] < 0 for order in orders):
            problems.append("negative amount_cents")
        if any(order["customer_id"] not in warehouse.dim_customer for order in orders):
            problems.append("unknown customer_id")
        if problems:
            raise QualityGateFailed(f"orders of {ctx.ds}: " + "; ".join(problems))

    def load_orders(ctx: Context) -> None:
        warehouse.fact_orders[ctx.ds] = list(warehouse.staging[ctx.ds])

    def daily_revenue(ctx: Context) -> None:
        orders = warehouse.fact_orders[ctx.ds]
        warehouse.daily_revenue[ctx.ds] = sum(order["amount_cents"] for order in orders)

    return DAG([
        Task("load_customers", load_customers, retries=1),
        Task("extract_orders", extract_orders, retries=2),
        Task("check_orders", check_orders, upstream=("extract_orders", "load_customers")),
        Task("load_orders", load_orders, upstream=("check_orders",)),
        Task("daily_revenue", daily_revenue, upstream=("load_orders",)),
    ])
