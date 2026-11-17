"""Stage 1 contract: unique_customers gives the same answers, fast."""

import pytest
import slow_pipeline as slow
from orders_data import budget, orders

from pipeline import unique_customers


@pytest.mark.parametrize("seed", range(5))
def test_same_answers_as_the_slow_version(seed):
    data = orders(500, customers=80, products=10, seed=seed)
    assert unique_customers(data) == slow.unique_customers(data)


def test_order_of_first_appearance():
    data = [{"customer": c} for c in ["b", "a", "b", "c", "a"]]
    assert unique_customers(data) == ["b", "a", "c"]
    assert unique_customers([]) == []


def test_fifty_thousand_orders_within_a_second():
    data = orders(50_000, customers=20_000, products=100)
    with budget(1.0, "unique_customers on 50 000 orders"):
        result = unique_customers(data)
    assert len(result) == len({order["customer"] for order in data})
