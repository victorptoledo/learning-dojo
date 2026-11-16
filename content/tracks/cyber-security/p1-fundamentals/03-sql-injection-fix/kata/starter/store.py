"""Data access for the shop. Every query here is built with f-strings: fix them."""

import sqlite3


def _product(row: tuple) -> dict:
    return {"id": row[0], "name": row[1], "price_cents": row[2]}


def find_user(conn: sqlite3.Connection, username: str) -> dict | None:
    """The user with exactly this username, or None."""
    row = conn.execute(
        f"SELECT id, username, role FROM users WHERE username = '{username}'"
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
    column = {"price": "price_cents", "newest": "created_at"}.get(sort, sort)
    direction = "DESC" if descending else "ASC"
    rows = conn.execute(
        f"SELECT id, name, price_cents FROM products WHERE name LIKE '%{term}%' "
        f"ORDER BY {column} {direction}, id LIMIT {limit}"
    ).fetchall()
    return [_product(row) for row in rows]


def products_by_ids(conn: sqlite3.Connection, ids: list[int]) -> list[dict]:
    """The products with these ids, in increasing id order."""
    joined = ", ".join(str(i) for i in ids)
    rows = conn.execute(
        f"SELECT id, name, price_cents FROM products WHERE id IN ({joined}) ORDER BY id"
    ).fetchall()
    return [_product(row) for row in rows]


def rename_product(conn: sqlite3.Connection, product_id: int, new_name: str) -> bool:
    """Rename a product; True if a row changed."""
    cursor = conn.execute(
        f"UPDATE products SET name = '{new_name}' WHERE id = {product_id}"
    )
    conn.commit()
    return cursor.rowcount > 0
