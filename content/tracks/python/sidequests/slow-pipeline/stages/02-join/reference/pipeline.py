"""The nightly orders pipeline. It gives the right answers; your job is to make it fast.

Do not change what any function returns: the contract tests compare the results with the
slow version's, and then time them.
"""

import json
from collections.abc import Callable, Iterable, Iterator

def unique_customers(orders: list[dict]) -> list[str]:
    """Customer ids in the order they first appear."""
    return list(dict.fromkeys(order["customer"] for order in orders))

def enrich(orders: list[dict], customers: list[dict]) -> list[dict]:
    """Each order with its customer's `country` added (`"unknown"` when the customer is missing)."""
    country: dict[str, str] = {}
    for customer in customers:
        country.setdefault(customer["id"], customer["country"])
    return [{**order, "country": country.get(order["customer"], "unknown")} for order in orders]

def top_products(orders: list[dict], n: int) -> list[tuple[str, int]]:
    """The `n` products sold in the largest quantity, with that quantity; ties by product name."""
    products = []
    for order in orders:
        if order["product"] not in products:
            products.append(order["product"])
    totals = []
    for product in products:
        total = sum(order["quantity"] for order in orders if order["product"] == product)
        totals.append((product, total))
    totals.sort(key=lambda item: (-item[1], item[0]))
    return totals[:n]

def read_orders(path: str) -> Iterator[dict]:
    """The orders of a JSON Lines file, one per line; blank lines are skipped. A line that is
    not JSON raises ValueError naming its line number."""
    with open(path, encoding="utf-8") as file:
        lines = file.readlines()
    orders = []
    for number, line in enumerate(lines, start=1):
        if not line.strip():
            continue
        try:
            orders.append(json.loads(line))
        except json.JSONDecodeError as error:
            raise ValueError(f"line {number}: {error}") from error
    return iter(orders)

def convert_totals(orders: Iterable[dict], rate: Callable[[str], float]) -> dict[str, float]:
    """Each customer's total in euros, rounded to cents. `rate(currency)` is the euro value of one
    unit of that currency; it asks a slow remote service, and its answers can change from one
    run to the next (so never between two calls of this function)."""
    totals = {}
    for order in orders:
        amount = order["quantity"] * order["price"] * rate(order["currency"])
        totals[order["customer"]] = totals.get(order["customer"], 0.0) + amount
    return {customer: round(total, 2) for customer, total in totals.items()}
