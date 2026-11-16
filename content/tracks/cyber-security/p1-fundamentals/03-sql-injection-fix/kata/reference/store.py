"""Data access for the shop, with every value bound and every identifier allow-listed."""

import sqlite3

SORT_COLUMNS = {"name": "name", "price": "price_cents", "newest": "created_at"}
MIN_LIMIT = 1
MAX_LIMIT = 100


def _product(row: tuple) -> dict:
    return {"id": row[0], "name": row[1], "price_cents": row[2]}


def _escape_like(term: str) -> str:
    return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")


def find_user(conn: sqlite3.Connection, username: str) -> dict | None:
    """The user with exactly this username, or None."""
    row = conn.execute(
        "SELECT id, username, role FROM users WHERE username = ?", (username,)
    ).fetchone()
    if row is None:
        return None
    return {"id": row[0], "username": row[1], "role": row[2]}


def search_products(
    conn: sqlite3.Connection,
    term: str,
    *,
    sort: str = "name",
    descending: bool = False,
    limit: int = 20,
) -> list[dict]:
    """Products whose name contains term, sorted by name, price or newest."""
    column = SORT_COLUMNS.get(sort)
    if column is None:
        raise ValueError(f"unknown sort: {sort!r}")
    if type(limit) is not int:
        raise ValueError("limit must be an integer")
    limit = max(MIN_LIMIT, min(MAX_LIMIT, limit))
    direction = "DESC" if descending else "ASC"
    pattern = f"%{_escape_like(term)}%"
    rows = conn.execute(
        "SELECT id, name, price_cents FROM products WHERE name LIKE ? ESCAPE '\\' "
        f"ORDER BY {column} {direction}, id LIMIT ?",
        (pattern, limit),
    ).fetchall()
    return [_product(row) for row in rows]


def products_by_ids(conn: sqlite3.Connection, ids: list[int]) -> list[dict]:
    """The products with these ids, in increasing id order."""
    if not ids:
        return []
    placeholders = ", ".join("?" for _ in ids)
    rows = conn.execute(
        f"SELECT id, name, price_cents FROM products WHERE id IN ({placeholders}) ORDER BY id",
        tuple(ids),
    ).fetchall()
    return [_product(row) for row in rows]


def rename_product(conn: sqlite3.Connection, product_id: int, new_name: str) -> bool:
    """Rename a product; True if a row changed."""
    cursor = conn.execute(
        "UPDATE products SET name = ? WHERE id = ?", (new_name, product_id)
    )
    conn.commit()
    return cursor.rowcount > 0
