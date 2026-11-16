"""A small sales warehouse on SQLite: customers from CSV, orders from JSON lines, a star."""

import csv
import json
import sqlite3
from dataclasses import dataclass, field
from datetime import UTC, datetime
from decimal import Decimal
from pathlib import Path

REQUIRED = ("order_id", "customer_id", "ordered_at", "lines")


@dataclass
class LoadReport:
    loaded: int = 0
    rejected: list[tuple[int, str]] = field(default_factory=list)


def create_schema(conn: sqlite3.Connection) -> None:
    """Create the star schema; running it again changes nothing."""
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS dim_customer (
            customer_key INTEGER PRIMARY KEY,
            customer_id  INTEGER NOT NULL UNIQUE,
            name         TEXT    NOT NULL,
            country      TEXT    NOT NULL
        );
        CREATE TABLE IF NOT EXISTS dim_date (
            date_key  INTEGER PRIMARY KEY,
            full_date TEXT    NOT NULL UNIQUE,
            year      INTEGER NOT NULL,
            month     INTEGER NOT NULL
        );
        CREATE TABLE IF NOT EXISTS fact_sales (
            order_id      INTEGER NOT NULL,
            line_no       INTEGER NOT NULL,
            date_key      INTEGER NOT NULL REFERENCES dim_date (date_key),
            customer_key  INTEGER NOT NULL REFERENCES dim_customer (customer_key),
            sku           TEXT    NOT NULL,
            quantity      INTEGER NOT NULL,
            revenue_cents INTEGER NOT NULL,
            PRIMARY KEY (order_id, line_no)
        );
        """
    )


def load_customers(conn: sqlite3.Connection, path: Path) -> int:
    """Upsert the customers of the CSV (type 1: changes overwrite); returns rows read."""
    with open(path, encoding="utf-8-sig", newline="") as source:
        rows = list(csv.DictReader(source))
    with conn:
        conn.executemany(
            """INSERT INTO dim_customer (customer_id, name, country) VALUES (?, ?, ?)
               ON CONFLICT (customer_id) DO UPDATE SET
                   name = excluded.name, country = excluded.country""",
            [(int(r["customer_id"]), r["name"].strip(), r["country"].strip()) for r in rows],
        )
    return len(rows)


def is_int(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def parse_lines(lines: object) -> list[tuple[str, int, int]] | None:
    """(sku, quantity, revenue in cents) per line, or None when a line is bad."""
    if not isinstance(lines, list) or not lines:
        return None
    parsed = []
    for line in lines:
        if not isinstance(line, dict):
            return None
        sku, quantity, price = line.get("sku"), line.get("quantity"), line.get("unit_price")
        if not isinstance(sku, str) or not sku.strip():
            return None
        if not is_int(quantity) or quantity <= 0:
            return None
        if is_int(price):
            price = Decimal(price)
        if not isinstance(price, Decimal) or price < 0:
            return None
        if price != price.quantize(Decimal("0.01")):
            return None
        parsed.append((sku.strip(), quantity, int(price * 100) * quantity))
    return parsed


def parse_order(text: str, known: dict[int, int]):
    """(order_id, customer_key, UTC date, lines) or the reason the record is rejected."""
    try:
        record = json.loads(text, parse_float=Decimal)
    except json.JSONDecodeError:
        return "invalid json"
    if not isinstance(record, dict) or any(key not in record for key in REQUIRED):
        return "missing field"
    if not is_int(record["order_id"]) or record["order_id"] <= 0:
        return "bad order_id"
    try:
        ordered_at = datetime.fromisoformat(record["ordered_at"])
    except (TypeError, ValueError):
        return "bad timestamp"
    if ordered_at.tzinfo is None:
        return "bad timestamp"
    lines = parse_lines(record["lines"])
    if lines is None:
        return "bad line"
    customer_key = known.get(record["customer_id"]) if is_int(record["customer_id"]) else None
    if customer_key is None:
        return "unknown customer"
    return record["order_id"], customer_key, ordered_at.astimezone(UTC).date(), lines


def load_orders(conn: sqlite3.Connection, path: Path) -> LoadReport:
    """Load the orders of a JSON-lines file; a record seen again replaces the order."""
    known = dict(conn.execute("SELECT customer_id, customer_key FROM dim_customer"))
    report = LoadReport()
    with open(path, encoding="utf-8") as source, conn:
        for number, text in enumerate(source, start=1):
            if not text.strip():
                continue
            result = parse_order(text, known)
            if isinstance(result, str):
                report.rejected.append((number, result))
                continue
            order_id, customer_key, day, lines = result
            date_key = int(day.strftime("%Y%m%d"))
            conn.execute(
                "INSERT OR IGNORE INTO dim_date VALUES (?, ?, ?, ?)",
                (date_key, day.isoformat(), day.year, day.month),
            )
            conn.execute("DELETE FROM fact_sales WHERE order_id = ?", (order_id,))
            conn.executemany(
                "INSERT INTO fact_sales VALUES (?, ?, ?, ?, ?, ?, ?)",
                [
                    (order_id, line_no, date_key, customer_key, sku, quantity, cents)
                    for line_no, (sku, quantity, cents) in enumerate(lines, start=1)
                ],
            )
            report.loaded += 1
    return report


def revenue_by_country(conn: sqlite3.Connection) -> list[tuple[str, int]]:
    """(country, revenue in cents), the largest first, then by country."""
    return conn.execute(
        """SELECT c.country, sum(f.revenue_cents) AS revenue
           FROM fact_sales AS f JOIN dim_customer AS c USING (customer_key)
           GROUP BY c.country ORDER BY revenue DESC, c.country"""
    ).fetchall()


def revenue_by_month(conn: sqlite3.Connection) -> list[tuple[int, int, int]]:
    """(year, month, revenue in cents) in calendar order."""
    return conn.execute(
        """SELECT d.year, d.month, sum(f.revenue_cents)
           FROM fact_sales AS f JOIN dim_date AS d USING (date_key)
           GROUP BY d.year, d.month ORDER BY d.year, d.month"""
    ).fetchall()
