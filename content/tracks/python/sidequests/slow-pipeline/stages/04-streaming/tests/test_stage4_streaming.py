"""Stage 4 contract: read_orders streams with little memory and behaves as before."""

import json
import tracemalloc

import pytest
import slow_pipeline as slow
from orders_data import orders

from pipeline import read_orders


def write(path, lines):
    path.write_text("".join(line + "\n" for line in lines), encoding="utf-8")


def test_same_orders_as_the_slow_version(tmp_path):
    path = tmp_path / "orders.jsonl"
    write(path, [json.dumps(o) for o in orders(300, 20, 10)] + ["", "  "])
    assert list(read_orders(str(path))) == list(slow.read_orders(str(path)))


def test_a_bad_line_fails_when_reached(tmp_path):
    path = tmp_path / "orders.jsonl"
    write(path, ['{"id": 1}', "", '{"id": 2}', "{broken", '{"id": 4}'])
    reader = read_orders(str(path))
    assert next(reader) == {"id": 1}
    assert next(reader) == {"id": 2}
    with pytest.raises(ValueError, match=r"^line 4: "):
        next(reader)


def test_two_hundred_thousand_orders_in_under_5_mb(tmp_path):
    path = tmp_path / "big.jsonl"
    with path.open("w", encoding="utf-8") as out:
        for order in orders(200_000, customers=5_000, products=500):
            out.write(json.dumps(order) + "\n")
    tracemalloc.start()
    try:
        count = sum(1 for _ in read_orders(str(path)))
        _, peak = tracemalloc.get_traced_memory()
    finally:
        tracemalloc.stop()
    assert count == 200_000
    assert peak < 5 * 1024 * 1024, f"peak memory was {peak / 1024 / 1024:.1f} MB; the budget is 5 MB"
