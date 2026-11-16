"""The accounts schema. Do not change this file."""

import sqlite3

SCHEMA = """
CREATE TABLE accounts (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    email TEXT NOT NULL,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL
);
"""


def connect() -> sqlite3.Connection:
    """A fresh in-memory database with the schema."""
    conn = sqlite3.connect(":memory:")
    conn.executescript(SCHEMA)
    return conn
