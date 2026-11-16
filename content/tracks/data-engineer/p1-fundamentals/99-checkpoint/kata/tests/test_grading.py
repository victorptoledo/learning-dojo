import json
import sqlite3
from pathlib import Path

import pytest

from warehouse import (
    create_schema,
    load_customers,
    load_orders,
    revenue_by_country,
    revenue_by_month,
)

DATA = Path(__file__).parent.parent / "data"


def warehouse():
    conn = sqlite3.connect(":memory:")
    create_schema(conn)
    load_customers(conn, DATA / "customers.csv")
    return conn


def order(**changes):
    record = {"order_id": 50, "customer_id": 1, "ordered_at": "2026-11-05T12:00:00+00:00",
              "lines": [{"sku": "KB-1", "quantity": 1, "unit_price": 80.00}]}
    record.update(changes)
    return json.dumps(record)


def load_text(conn, tmp_path, *lines):
    path = tmp_path / "orders.jsonl"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return load_orders(conn, path)


def facts(conn):
    return conn.execute(
        """SELECT f.order_id, f.line_no, d.full_date, c.customer_id, f.sku, f.quantity,
                  f.revenue_cents
           FROM fact_sales AS f
           JOIN dim_date AS d USING (date_key)
           JOIN dim_customer AS c USING (customer_key)
           ORDER BY f.order_id, f.line_no"""
    ).fetchall()


def columns(conn, table):
    return [row[1] for row in conn.execute(f"PRAGMA table_info({table})")]


def test_the_schema_has_the_tables_and_columns():
    conn = sqlite3.connect(":memory:")
    create_schema(conn)
    create_schema(conn)
    assert columns(conn, "dim_customer") == ["customer_key", "customer_id", "name", "country"]
    assert columns(conn, "dim_date") == ["date_key", "full_date", "year", "month"]
    assert columns(conn, "fact_sales") == ["order_id", "line_no", "date_key", "customer_key",
                                           "sku", "quantity", "revenue_cents"]


def test_the_fact_table_has_foreign_keys_and_an_order_line_key():
    conn = warehouse()
    conn.execute("PRAGMA foreign_keys = ON")
    with pytest.raises(sqlite3.IntegrityError):
        conn.execute("INSERT INTO fact_sales VALUES (1, 1, 19990101, 1, 'X', 1, 1)")
    conn.execute("INSERT INTO dim_date VALUES (20260101, '2026-01-01', 2026, 1)")
    conn.execute("INSERT INTO fact_sales VALUES (1, 1, 20260101, 1, 'X', 1, 1)")
    with pytest.raises(sqlite3.IntegrityError):
        conn.execute("INSERT INTO fact_sales VALUES (1, 1, 20260101, 1, 'Y', 1, 1)")
    with pytest.raises(sqlite3.IntegrityError):
        conn.execute("INSERT INTO fact_sales VALUES (2, 1, 20260101, 999, 'X', 1, 1)")


def test_the_sample_orders_end_in_the_star():
    conn = warehouse()
    load_orders(conn, DATA / "orders.jsonl")
    assert facts(conn) == [
        (1, 1, "2026-11-01", 1, "KB-1", 1, 8000),
        (2, 1, "2026-11-02", 2, "IDE-1", 1, 12000),
        (8, 1, "2026-11-03", 3, "CRS-1", 1, 29999),
    ]


def test_the_sample_rejects_have_line_numbers_and_reasons():
    conn = warehouse()
    report = load_orders(conn, DATA / "orders.jsonl")
    assert report.rejected == [
        (3, "invalid json"),
        (4, "unknown customer"),
        (5, "bad timestamp"),
        (6, "bad line"),
        (7, "missing field"),
    ]


def test_loading_the_same_file_twice_changes_nothing():
    conn = warehouse()
    load_orders(conn, DATA / "orders.jsonl")
    first = facts(conn)
    load_orders(conn, DATA / "orders.jsonl")
    assert facts(conn) == first
    assert conn.execute("SELECT count(*) FROM dim_date").fetchone() == (3,)


def test_dates_are_utc_days(tmp_path):
    conn = warehouse()
    load_text(conn, tmp_path, order(ordered_at="2026-12-31T22:00:00-05:00"))
    assert facts(conn)[0][2] == "2027-01-01"
    assert conn.execute("SELECT date_key, year, month FROM dim_date").fetchall() == [
        (20270101, 2027, 1)
    ]


def test_money_is_exact_cents(tmp_path):
    conn = warehouse()
    load_text(conn, tmp_path, order(lines=[{"sku": "A", "quantity": 3, "unit_price": 0.1},
                                           {"sku": "B", "quantity": 1, "unit_price": 19.99}]))
    assert [row[6] for row in facts(conn)] == [30, 1999]


@pytest.mark.parametrize(
    ("record", "reason"),
    [
        ("[1, 2, 3]", "missing field"),
        (order(order_id="50"), "bad order_id"),
        (order(order_id=True), "bad order_id"),
        (order(ordered_at="yesterday"), "bad timestamp"),
        (order(ordered_at=20261105), "bad timestamp"),
        (order(lines=[]), "bad line"),
        (order(lines=[{"sku": "A", "quantity": 1, "unit_price": "80.00"}]), "bad line"),
        (order(lines=[{"sku": "A", "quantity": 1, "unit_price": 1.005}]), "bad line"),
        (order(lines=[{"sku": "A", "quantity": 1, "unit_price": -1}]), "bad line"),
        (order(lines=[{"sku": "A", "quantity": 1.5, "unit_price": 1}]), "bad line"),
        (order(lines=[{"sku": " ", "quantity": 1, "unit_price": 1}]), "bad line"),
        (order(lines=[{"sku": "A", "unit_price": 1}]), "bad line"),
        (order(customer_id="1"), "unknown customer"),
    ],
)
def test_bad_records_are_rejected(tmp_path, record, reason):
    conn = warehouse()
    report = load_text(conn, tmp_path, record)
    assert report.loaded == 0
    assert report.rejected == [(1, reason)]
    assert facts(conn) == []


def test_a_record_seen_again_replaces_the_whole_order(tmp_path):
    conn = warehouse()
    two_lines = [{"sku": "A", "quantity": 1, "unit_price": 1}, {"sku": "B", "quantity": 1,
                                                                 "unit_price": 2}]
    load_text(conn, tmp_path, order(lines=two_lines))
    load_text(conn, tmp_path, order(lines=[{"sku": "C", "quantity": 2, "unit_price": 5}]))
    assert facts(conn) == [(50, 1, "2026-11-05", 1, "C", 2, 1000)]


def test_one_bad_record_does_not_stop_the_others(tmp_path):
    conn = warehouse()
    report = load_text(conn, tmp_path, "{", order(order_id=51), "", order(order_id=52))
    assert report.loaded == 2
    assert report.rejected == [(1, "invalid json")]


def test_customers_are_upserted_with_stable_keys(tmp_path):
    conn = warehouse()
    load_orders(conn, DATA / "orders.jsonl")
    keys = dict(conn.execute("SELECT customer_id, customer_key FROM dim_customer"))
    moved = tmp_path / "customers.csv"
    moved.write_text("customer_id,name,country\n1,Ada King,IE\n4,Alan Turing,GB\n",
                     encoding="utf-8")
    assert load_customers(conn, moved) == 2
    rows = conn.execute(
        "SELECT customer_id, customer_key, name, country FROM dim_customer ORDER BY customer_id"
    ).fetchall()
    assert [r[0] for r in rows] == [1, 2, 3, 4]
    assert rows[0] == (1, keys[1], "Ada King", "IE")
    assert revenue_by_country(conn) == [("FI", 29999), ("US", 12000), ("IE", 8000)]


def test_revenue_by_country_breaks_ties_by_country(tmp_path):
    conn = warehouse()
    load_text(conn, tmp_path, order(order_id=1, customer_id=2), order(order_id=2, customer_id=1))
    assert revenue_by_country(conn) == [("GB", 8000), ("US", 8000)]


def test_revenue_by_month(tmp_path):
    conn = warehouse()
    load_orders(conn, DATA / "orders.jsonl")
    load_text(conn, tmp_path, order(ordered_at="2026-10-31T23:59:00+00:00"))
    assert revenue_by_month(conn) == [(2026, 10, 8000), (2026, 11, 49999)]


def test_load_orders_commits(tmp_path):
    path = tmp_path / "w.db"
    conn = sqlite3.connect(path)
    create_schema(conn)
    load_customers(conn, DATA / "customers.csv")
    load_orders(conn, DATA / "orders.jsonl")
    other = sqlite3.connect(path)
    assert other.execute("SELECT count(*) FROM fact_sales").fetchone() == (3,)
