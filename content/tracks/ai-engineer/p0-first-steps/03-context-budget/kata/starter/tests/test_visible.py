from decimal import Decimal

from budget import Message, Price, estimate_cost, fit_history


def test_keeps_everything_that_fits():
    messages = [Message("user", "Hi there"), Message("assistant", "Hello! How can I help?")]
    assert fit_history("Be brief.", messages, window=100, reserve=20) == messages


def test_prices_a_request():
    assert estimate_cost(1_000, 500, Price(Decimal("3"), Decimal("15"))) == Decimal("0.010500")
