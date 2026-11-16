"""A first batch pipeline: extract a CSV of orders, clean it, load it into SQLite."""

import csv
import re
import sqlite3
from dataclasses import dataclass
from datetime import date
from decimal import Decimal, InvalidOperation
from pathlib import Path

COUNTRY = re.compile(r"[A-Z]{2}")


@dataclass(frozen=True)
class Order:
    order_id: int
    customer: str
    amount_cents: int
    country: str
    order_date: date


def extract(path: Path) -> list[dict[str, str]]:
    """Every data row of the CSV as a dict of column -> raw text."""
    with open(path, encoding="utf-8-sig", newline="") as source:
        return list(csv.DictReader(source))


def parse(row: dict[str, str]) -> Order | str:
    """The clean order, or the reason the row is rejected."""
    try:
        order_id = int((row.get("order_id") or "").strip())
    except ValueError:
        return "bad order_id"
    if order_id <= 0:
        return "bad order_id"

    customer = (row.get("customer") or "").strip()
    if not customer:
        return "missing customer"

    try:
        amount = Decimal((row.get("amount") or "").strip())
    except InvalidOperation:
        return "bad amount"
    if not amount.is_finite() or amount < 0 or amount != amount.quantize(Decimal("0.01")):
        return "bad amount"

    country = (row.get("country") or "").strip().upper()
    if COUNTRY.fullmatch(country) is None:
        return "bad country"

    try:
        order_date = date.fromisoformat((row.get("order_date") or "").strip())
    except ValueError:
        return "bad order_date"

    return Order(order_id, customer, int(amount * 100), country, order_date)


def transform(rows: list[dict[str, str]]) -> tuple[list[Order], list[tuple[int, str]]]:
    """Clean orders, and (line number, reason) for each rejected row."""
    orders: list[Order] = []
    rejected: list[tuple[int, str]] = []
    seen: set[int] = set()
    for index, row in enumerate(rows):
        line = index + 2  # line 1 is the header
        result = parse(row)
        if isinstance(result, str):
            rejected.append((line, result))
        elif result.order_id in seen:
            rejected.append((line, "duplicate order_id"))
        else:
            seen.add(result.order_id)
            orders.append(result)
    return orders, rejected


def load(conn: sqlite3.Connection, orders: list[Order]) -> int:
    """Upsert the orders into the orders table in one transaction; returns rows written."""
    with conn:
        conn.execute(
            """CREATE TABLE IF NOT EXISTS orders (
                   order_id     INTEGER PRIMARY KEY,
                   customer     TEXT    NOT NULL,
                   amount_cents INTEGER NOT NULL,
                   country      TEXT    NOT NULL,
                   order_date   TEXT    NOT NULL
               )"""
        )
        conn.executemany(
            """INSERT INTO orders (order_id, customer, amount_cents, country, order_date)
               VALUES (?, ?, ?, ?, ?)
               ON CONFLICT (order_id) DO UPDATE SET
                   customer = excluded.customer,
                   amount_cents = excluded.amount_cents,
                   country = excluded.country,
                   order_date = excluded.order_date""",
            [
                (o.order_id, o.customer, o.amount_cents, o.country, o.order_date.isoformat())
                for o in orders
            ],
        )
    return len(orders)


def run(csv_path: Path, db_path: Path) -> dict[str, int]:
    """Extract, transform and load; returns how many rows were read, loaded and rejected."""
    rows = extract(csv_path)
    orders, rejected = transform(rows)
    conn = sqlite3.connect(db_path)
    try:
        loaded = load(conn, orders)
    finally:
        conn.close()
    return {"read": len(rows), "loaded": loaded, "rejected": len(rejected)}
