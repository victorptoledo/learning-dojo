"""A small sales warehouse on SQLite: customers from CSV, orders from JSON lines, a star."""

import sqlite3
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class LoadReport:
    loaded: int = 0
    rejected: list[tuple[int, str]] = field(default_factory=list)


def create_schema(conn: sqlite3.Connection) -> None:
    """Create the star schema; running it again changes nothing."""
    raise NotImplementedError("todo: write create_schema()")


def load_customers(conn: sqlite3.Connection, path: Path) -> int:
    """Upsert the customers of the CSV (type 1: changes overwrite); returns rows read."""
    raise NotImplementedError("todo: write load_customers()")


def load_orders(conn: sqlite3.Connection, path: Path) -> LoadReport:
    """Load the orders of a JSON-lines file; a record seen again replaces the order."""
    raise NotImplementedError("todo: write load_orders()")


def revenue_by_country(conn: sqlite3.Connection) -> list[tuple[str, int]]:
    """(country, revenue in cents), the largest first, then by country."""
    raise NotImplementedError("todo: write revenue_by_country()")


def revenue_by_month(conn: sqlite3.Connection) -> list[tuple[int, int, int]]:
    """(year, month, revenue in cents) in calendar order."""
    raise NotImplementedError("todo: write revenue_by_month()")
