"""Stage 2 contract: enrich gives the same answers, fast."""

import copy

import pytest
import slow_pipeline as slow
from orders_data import budget, customers, orders

from pipeline import enrich


@pytest.mark.parametrize("seed", range(5))
def test_same_answers_as_the_slow_version(seed):
    data = orders(400, customers=120, products=10, seed=seed)
    people = customers(100, seed=seed)
    assert enrich(data, people) == slow.enrich(data, people)


def test_missing_customers_are_unknown_and_the_first_duplicate_wins():
    people = [{"id": "a", "country": "BR"}, {"id": "a", "country": "PT"}]
    result = enrich([{"customer": "a"}, {"customer": "zzz"}], people)
    assert result == [{"customer": "a", "country": "BR"}, {"customer": "zzz", "country": "unknown"}]


def test_the_inputs_are_left_untouched():
    data = orders(50, customers=10, products=5)
    people = customers(10)
    before = copy.deepcopy(data)
    enrich(data, people)
    assert data == before


def test_twenty_thousand_orders_within_a_second():
    data = orders(20_000, customers=12_000, products=100)
    people = customers(10_000)
    with budget(1.0, "enrich on 20 000 orders and 10 000 customers"):
        result = enrich(data, people)
    assert len(result) == 20_000
