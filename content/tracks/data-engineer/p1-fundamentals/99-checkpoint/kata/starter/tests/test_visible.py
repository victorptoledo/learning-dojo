import sqlite3
from pathlib import Path

from warehouse import create_schema, load_customers, load_orders, revenue_by_country

DATA = Path(__file__).parent.parent / "data"


def test_the_sample_files_load_into_the_star():
    conn = sqlite3.connect(":memory:")
    create_schema(conn)
    assert load_customers(conn, DATA / "customers.csv") == 3
    report = load_orders(conn, DATA / "orders.jsonl")
    assert report.loaded == 4
    assert revenue_by_country(conn) == [("FI", 29999), ("US", 12000), ("GB", 8000)]
