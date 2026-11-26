"""The catalog of a small warehouse: models, their columns and their owners.

Given; do not change it.
"""

from dataclasses import dataclass, field

TRANSFORMS = ("copy", "derive", "hash", "aggregate")


@dataclass(frozen=True)
class Column:
    """A column and the upstream columns ("model.column") it is computed from."""

    name: str
    sources: tuple[str, ...] = ()
    transform: str = "copy"  # one of TRANSFORMS

    def __post_init__(self):
        if self.transform not in TRANSFORMS:
            raise ValueError(f"unknown transform {self.transform!r}")


@dataclass(frozen=True)
class Model:
    """A table of the warehouse. A model without upstream models is a source (raw data)."""

    name: str
    upstream: tuple[str, ...] = ()
    columns: tuple[Column, ...] = ()
    owner: str = ""
    product: bool = False  # published to consumers, with a freshness objective
    freshness_hours: int | None = None


@dataclass(frozen=True)
class Grant:
    """What a role may read: models, personal data or not, and an optional row filter."""

    models: frozenset[str] = field(default_factory=frozenset)
    pii: bool = False
    row_filter: tuple[str, object] | None = None  # (column, value) the rows must have


SHOP = (
    Model("raw.customers", columns=(
        Column("customer_id"), Column("email"), Column("name"), Column("country"),
    ), owner="crm-team"),
    Model("raw.orders", columns=(
        Column("order_id"), Column("customer_id"), Column("amount_cents"),
        Column("ordered_at"),
    ), owner="shop-team"),
    Model("stg_customers", upstream=("raw.customers",), columns=(
        Column("customer_id", ("raw.customers.customer_id",)),
        Column("email_hash", ("raw.customers.email",), "hash"),
        Column("name", ("raw.customers.name",)),
        Column("country", ("raw.customers.country",)),
    ), owner="data-platform"),
    Model("stg_orders", upstream=("raw.orders",), columns=(
        Column("order_id", ("raw.orders.order_id",)),
        Column("customer_id", ("raw.orders.customer_id",)),
        Column("amount_cents", ("raw.orders.amount_cents",)),
        Column("order_date", ("raw.orders.ordered_at",), "derive"),
    ), owner="data-platform"),
    Model("dim_customer", upstream=("stg_customers",), columns=(
        Column("customer_id", ("stg_customers.customer_id",)),
        Column("email_hash", ("stg_customers.email_hash",)),
        Column("display_name", ("stg_customers.name", "stg_customers.country"), "derive"),
        Column("country", ("stg_customers.country",)),
    ), owner="analytics", product=True, freshness_hours=24),
    Model("fct_sales", upstream=("stg_orders", "dim_customer"), columns=(
        Column("order_id", ("stg_orders.order_id",)),
        Column("customer_id", ("stg_orders.customer_id",)),
        Column("country", ("dim_customer.country",)),
        Column("amount_cents", ("stg_orders.amount_cents",)),
        Column("order_date", ("stg_orders.order_date",)),
    ), owner="analytics", product=True, freshness_hours=6),
    Model("daily_revenue", upstream=("fct_sales",), columns=(
        Column("order_date", ("fct_sales.order_date",)),
        Column("country", ("fct_sales.country",)),
        Column("customers", ("fct_sales.customer_id",), "aggregate"),
        Column("revenue_cents", ("fct_sales.amount_cents",), "aggregate"),
    ), owner="finance", product=True, freshness_hours=12),
)
