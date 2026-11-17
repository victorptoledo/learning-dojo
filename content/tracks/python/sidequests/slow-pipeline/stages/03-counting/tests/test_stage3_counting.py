"""Stage 3 contract: top_products gives the same answers, fast."""

import pytest
import slow_pipeline as slow
from orders_data import budget, orders

from pipeline import top_products


@pytest.mark.parametrize(("seed", "n"), [(0, 1), (1, 5), (2, 10), (3, 50), (4, 0)])
def test_same_answers_as_the_slow_version(seed, n):
    data = orders(600, customers=50, products=40, seed=seed)
    assert top_products(data, n) == slow.top_products(data, n)


def test_ties_go_by_product_name():
    data = [{"product": p, "quantity": q} for p, q in [("b", 2), ("a", 2), ("c", 5), ("a", 1), ("b", 1)]]
    assert top_products(data, 3) == [("c", 5), ("a", 3), ("b", 3)]


def test_fifty_thousand_orders_within_a_second():
    data = orders(50_000, customers=1_000, products=5_000)
    with budget(1.0, "top_products on 50 000 orders"):
        result = top_products(data, 10)
    assert len(result) == 10
