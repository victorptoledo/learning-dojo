import sqlite3
from datetime import date
from pathlib import Path

import pytest

from pipeline import Order, extract, load, run, transform

DATA = Path(__file__).parent.parent / "data" / "orders.csv"

GOOD = {"order_id": "1", "customer": "Ada", "amount": "1.00", "country": "GB",
        "order_date": "2026-11-01"}


def row(**changes):
    return {**GOOD, **changes}


def reason(**changes):
    orders, rejected = transform([row(**changes)])
    assert orders == []
    return rejected[0][1]


def table(conn):
    return conn.execute(
        "SELECT order_id, customer, amount_cents, country, order_date FROM orders ORDER BY order_id"
    ).fetchall()


def test_extract_strips_a_byte_order_mark(tmp_path):
    path = tmp_path / "bom.csv"
    path.write_text("\ufefforder_id,customer\n1,Ada\n", encoding="utf-8")
    assert extract(path) == [{"order_id": "1", "customer": "Ada"}]


def test_extract_keeps_quoted_commas(tmp_path):
    path = tmp_path / "quoted.csv"
    path.write_text('order_id,customer\n1,"Lovelace, Ada"\n', encoding="utf-8")
    assert extract(path) == [{"order_id": "1", "customer": "Lovelace, Ada"}]


@pytest.mark.parametrize(
    ("changes", "expected"),
    [
        ({"order_id": "abc"}, "bad order_id"),
        ({"order_id": "0"}, "bad order_id"),
        ({"order_id": ""}, "bad order_id"),
        ({"customer": "   "}, "missing customer"),
        ({"amount": "twelve"}, "bad amount"),
        ({"amount": "-1"}, "bad amount"),
        ({"amount": "1.005"}, "bad amount"),
        ({"amount": "NaN"}, "bad amount"),
        ({"country": "USA"}, "bad country"),
        ({"country": "U1"}, "bad country"),
        ({"order_date": "2026-13-01"}, "bad order_date"),
        ({"order_date": "01/11/2026"}, "bad order_date"),
    ],
)
def test_transform_rejects_bad_fields(changes, expected):
    assert reason(**changes) == expected


def test_the_first_bad_field_is_the_reason():
    assert reason(order_id="x", amount="y", country="zzz") == "bad order_id"
    assert reason(amount="y", country="zzz") == "bad amount"


def test_whole_amounts_and_cents():
    orders, _ = transform([row(amount="5"), row(order_id="2", amount="0.99")])
    assert [o.amount_cents for o in orders] == [500, 99]


def test_a_repeated_order_id_keeps_the_first_and_rejects_the_rest():
    orders, rejected = transform([row(), row(customer="Grace"), row(order_id="2")])
    assert [o.customer for o in orders] == ["Ada", "Ada"]
    assert [o.order_id for o in orders] == [1, 2]
    assert rejected == [(3, "duplicate order_id")]


def test_a_rejected_row_does_not_claim_its_order_id():
    orders, rejected = transform([row(amount="bad"), row(customer="Grace")])
    assert orders == [Order(1, "Grace", 100, "GB", date(2026, 11, 1))]
    assert rejected == [(2, "bad amount")]


def test_transform_reports_the_csv_line_numbers():
    _, rejected = transform(extract(DATA))
    assert rejected == [
        (5, "missing customer"),
        (6, "bad amount"),
        (7, "bad amount"),
        (8, "bad order_id"),
        (9, "bad country"),
        (10, "bad order_date"),
        (11, "duplicate order_id"),
    ]


def test_load_creates_the_table_and_writes_the_rows():
    conn = sqlite3.connect(":memory:")
    written = load(conn, [Order(7, "Ada", 1990, "GB", date(2026, 11, 1))])
    assert written == 1
    assert table(conn) == [(7, "Ada", 1990, "GB", "2026-11-01")]


def test_loading_twice_is_idempotent():
    conn = sqlite3.connect(":memory:")
    orders = [Order(1, "Ada", 100, "GB", date(2026, 11, 1)),
              Order(2, "Grace", 200, "US", date(2026, 11, 2))]
    load(conn, orders)
    load(conn, orders)
    assert len(table(conn)) == 2


def test_a_reload_updates_changed_rows():
    conn = sqlite3.connect(":memory:")
    load(conn, [Order(1, "Ada", 100, "GB", date(2026, 11, 1))])
    load(conn, [Order(1, "Ada King", 150, "GB", date(2026, 11, 2))])
    assert table(conn) == [(1, "Ada King", 150, "GB", "2026-11-02")]


def test_load_commits(tmp_path):
    path = tmp_path / "w.db"
    conn = sqlite3.connect(path)
    load(conn, [Order(1, "Ada", 100, "GB", date(2026, 11, 1))])
    other = sqlite3.connect(path)
    assert table(other) == [(1, "Ada", 100, "GB", "2026-11-01")]


def test_the_primary_key_is_order_id():
    conn = sqlite3.connect(":memory:")
    load(conn, [])
    columns = {name: pk for _, name, _, _, _, pk in conn.execute("PRAGMA table_info(orders)")}
    assert columns == {"order_id": 1, "customer": 0, "amount_cents": 0, "country": 0,
                       "order_date": 0}


def test_run_reports_and_reruns_safely(tmp_path):
    db = tmp_path / "warehouse.db"
    assert run(DATA, db) == {"read": 11, "loaded": 4, "rejected": 7}
    assert run(DATA, db) == {"read": 11, "loaded": 4, "rejected": 7}
    conn = sqlite3.connect(db)
    assert table(conn) == [
        (1001, "Ada Lovelace", 1990, "GB", "2026-11-01"),
        (1002, "Grace Hopper", 500, "US", "2026-11-01"),
        (1003, "Linus", 12000, "FI", "2026-11-02"),
        (1009, "Frances Allen", 99, "US", "2026-11-04"),
    ]
