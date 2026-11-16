from datetime import date
from pathlib import Path

from pipeline import Order, extract, transform

DATA = Path(__file__).parent.parent / "data" / "orders.csv"


def test_extract_reads_every_data_row():
    rows = extract(DATA)
    assert len(rows) == 11
    assert rows[0] == {
        "order_id": "1001",
        "customer": "Ada Lovelace",
        "amount": "19.90",
        "country": "gb",
        "order_date": "2026-11-01",
    }


def test_transform_cleans_a_good_row():
    orders, rejected = transform(
        [{"order_id": " 7 ", "customer": " Ada ", "amount": "19.9", "country": "gb",
          "order_date": "2026-11-01"}]
    )
    assert orders == [Order(7, "Ada", 1990, "GB", date(2026, 11, 1))]
    assert rejected == []
