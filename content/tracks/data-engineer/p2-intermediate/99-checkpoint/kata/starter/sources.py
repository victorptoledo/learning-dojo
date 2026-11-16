"""The systems the pipeline talks to. Do not change this file: the tests use their own."""

from dataclasses import dataclass, field
from datetime import date
from typing import Protocol


class Source(Protocol):
    def customers(self) -> list[dict]:
        """Every customer: {"customer_id", "name", "country"}."""
        ...

    def orders_for(self, ds: date) -> list[dict]:
        """The orders of one day: {"order_id", "customer_id", "amount_cents"}.
        May raise ConnectionError: the API is flaky."""
        ...


@dataclass
class Warehouse:
    dim_customer: dict[int, dict] = field(default_factory=dict)  # customer_id -> row
    staging: dict[date, list[dict]] = field(default_factory=dict)  # ds -> extracted orders
    fact_orders: dict[date, list[dict]] = field(default_factory=dict)  # ds -> partition
    daily_revenue: dict[date, int] = field(default_factory=dict)  # ds -> cents
