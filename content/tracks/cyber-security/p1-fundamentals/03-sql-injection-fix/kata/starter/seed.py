"""A small in-memory shop database for the tests. Do not change this file."""

import sqlite3

SCHEMA = """
CREATE TABLE users (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL
);
CREATE TABLE products (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    price_cents INTEGER NOT NULL,
    created_at TEXT NOT NULL
);
"""

USERS = [
    (1, "ada", "admin"),
    (2, "grace", "customer"),
    (3, "linus", "customer"),
]

PRODUCTS = [
    (1, "Blue Mug", 1200, "2026-01-10"),
    (2, "Red Mug", 1100, "2026-03-02"),
    (3, "Mug Rack", 3500, "2026-02-14"),
    (4, "100% Cotton Tee", 2000, "2026-04-01"),
    (5, "snake_case Sticker", 300, "2026-05-20"),
    (6, "Green Tea", 900, "2026-03-02"),
    (7, "Back\\slash Poster", 1500, "2026-06-30"),
]


def demo_db() -> sqlite3.Connection:
    """A fresh in-memory database with the schema and the demo rows."""
    conn = sqlite3.connect(":memory:")
    conn.executescript(SCHEMA)
    conn.executemany("INSERT INTO users VALUES (?, ?, ?)", USERS)
    conn.executemany("INSERT INTO products VALUES (?, ?, ?, ?)", PRODUCTS)
    conn.commit()
    return conn
