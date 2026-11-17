"""Stage 5 contract: convert_totals calls the slow rate service once per currency per run."""

import time

import pytest
import slow_pipeline as slow
from orders_data import budget, orders

RATES = {"EUR": 1.0, "USD": 0.92, "BRL": 0.17, "GBP": 1.15}


class SlowRates:
    def __init__(self, rates: dict[str, float], delay: float = 0.0) -> None:
        self.rates = rates
        self.delay = delay
        self.calls: list[str] = []

    def __call__(self, currency: str) -> float:
        self.calls.append(currency)
        time.sleep(self.delay)
        return self.rates[currency]


@pytest.mark.parametrize("seed", range(5))
def test_same_totals_as_the_slow_version(seed):
    from pipeline import convert_totals

    data = orders(300, customers=25, products=10, seed=seed)
    assert convert_totals(data, SlowRates(RATES)) == slow.convert_totals(data, SlowRates(RATES))


def test_one_call_per_currency():
    from pipeline import convert_totals

    rates = SlowRates(RATES)
    convert_totals(orders(500, customers=10, products=5), rates)
    assert sorted(rates.calls) == sorted(RATES), "each currency asked exactly once"


def test_rates_are_not_kept_between_runs():
    from pipeline import convert_totals

    data = [{"customer": "a", "quantity": 1, "price": 10.0, "currency": "USD"}]
    assert convert_totals(data, SlowRates({"USD": 0.9})) == {"a": 9.0}
    assert convert_totals(data, SlowRates({"USD": 0.5})) == {"a": 5.0}


def test_two_thousand_orders_within_a_second():
    from pipeline import convert_totals

    rates = SlowRates(RATES, delay=0.01)
    with budget(1.0, "convert_totals on 2 000 orders with a 10 ms rate service"):
        totals = convert_totals(orders(2_000, customers=100, products=10), rates)
    assert len(totals) == 100
