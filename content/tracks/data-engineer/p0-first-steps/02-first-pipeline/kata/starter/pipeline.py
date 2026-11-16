"""A first batch pipeline: extract a CSV of orders, clean it, load it into SQLite."""

import sqlite3
from dataclasses import dataclass
from datetime import date
from pathlib import Path


@dataclass(frozen=True)
class Order:
    order_id: int
    customer: str
    amount_cents: int
    country: str
    order_date: date


def extract(path: Path) -> list[dict[str, str]]:
    """Every data row of the CSV as a dict of column -> raw text."""
    raise NotImplementedError("todo: write extract()")


def transform(rows: list[dict[str, str]]) -> tuple[list[Order], list[tuple[int, str]]]:
    """Clean orders, and (line number, reason) for each rejected row."""
    raise NotImplementedError("todo: write transform()")


def load(conn: sqlite3.Connection, orders: list[Order]) -> int:
    """Upsert the orders into the orders table in one transaction; returns rows written."""
    raise NotImplementedError("todo: write load()")


def run(csv_path: Path, db_path: Path) -> dict[str, int]:
    """Extract, transform and load; returns how many rows were read, loaded and rejected."""
    raise NotImplementedError("todo: write run()")
