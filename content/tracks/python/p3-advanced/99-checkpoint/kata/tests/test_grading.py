import asyncio
import time

import pytest

from quotes import CurrencyMismatch, Line, Money, Quote, quote


class FakeSource:
    """A price source with a fixed price list, a delay per call and an optional failure."""

    def __init__(self, name, price_list, *, delay=0.0, error=None, tracker=None):
        self.name = name
        self.price_list = price_list
        self.delay = delay
        self.error = error
        self.tracker = tracker if tracker is not None else Tracker()
        self.calls = []

    async def prices(self, skus):
        self.calls.append(list(skus))
        self.tracker.enter()
        try:
            await asyncio.sleep(self.delay)
            if self.error is not None:
                raise self.error
            return {sku: self.price_list[sku] for sku in skus if sku in self.price_list}
        except asyncio.CancelledError:
            self.tracker.cancelled += 1
            raise
        finally:
            self.tracker.leave()


class Tracker:
    def __init__(self):
        self.active = 0
        self.max_active = 0
        self.cancelled = 0

    def enter(self):
        self.active += 1
        self.max_active = max(self.max_active, self.active)

    def leave(self):
        self.active -= 1


def eur(cents):
    return Money(cents, "EUR")


# ---- Money ------------------------------------------------------------------------


def test_money_adds_and_sums_from_zero():
    assert eur(150) + eur(250) == eur(400)
    assert sum([eur(100), eur(20), eur(3)]) == eur(123)
    assert sum([], eur(0)) == eur(0)


def test_money_multiplies_by_an_int_on_both_sides():
    assert eur(250) * 3 == eur(750)
    assert 3 * eur(250) == eur(750)


def test_money_refuses_floats_and_strings_with_type_error():
    with pytest.raises(TypeError):
        eur(100) * 1.5
    with pytest.raises(TypeError):
        eur(100) + 1
    with pytest.raises(TypeError):
        "1" + eur(100)
    with pytest.raises(TypeError):
        eur(100) < 5


def test_money_refuses_to_mix_currencies():
    with pytest.raises(CurrencyMismatch):
        eur(100) + Money(100, "USD")
    with pytest.raises(CurrencyMismatch):
        eur(100) < Money(100, "USD")
    assert issubclass(CurrencyMismatch, ValueError)


def test_money_orders_hashes_and_formats():
    assert eur(99) < eur(100) <= eur(100) < eur(101)
    assert max([eur(5), eur(50), eur(7)]) == eur(50)
    assert len({eur(100), eur(100), Money(100, "USD")}) == 2
    assert eur(100) != Money(100, "USD")
    assert f"{eur(123456)}" == "1234.56 EUR"
    assert str(eur(-5)) == "-0.05 EUR"
    assert f"{eur(123456):,.1f}" == "1,234.6 EUR"


def test_money_is_immutable():
    price = eur(100)
    with pytest.raises(AttributeError):
        price.cents = 5


# ---- quote ------------------------------------------------------------------------


def test_quote_picks_the_cheapest_source_per_sku_and_totals():
    a = FakeSource("a", {"apple": eur(50), "pear": eur(90)})
    b = FakeSource("b", {"apple": eur(40), "pear": eur(95), "fig": eur(300)})
    result = asyncio.run(quote({"pear": 2, "apple": 3, "fig": 1}, [a, b]))
    assert isinstance(result, Quote)
    assert result.lines == (
        Line("apple", 3, eur(40), "b"),
        Line("fig", 1, eur(300), "b"),
        Line("pear", 2, eur(90), "a"),
    )
    assert result.total == eur(3 * 40 + 300 + 2 * 90)
    assert result.missing == frozenset()
    assert result.failed == ()


def test_a_tie_goes_to_the_source_listed_first():
    a = FakeSource("a", {"x": eur(10)})
    b = FakeSource("b", {"x": eur(10)})
    assert asyncio.run(quote({"x": 1}, [b, a])).lines[0].source == "b"
    assert asyncio.run(quote({"x": 1}, [a, b])).lines[0].source == "a"


def test_unpriced_skus_are_missing_and_an_empty_quote_is_zero():
    a = FakeSource("a", {"x": eur(10)})
    result = asyncio.run(quote({"x": 1, "y": 2}, [a]))
    assert result.missing == frozenset({"y"})
    assert result.total == eur(10)
    empty = asyncio.run(quote({}, [a], currency="USD"))
    assert empty.total == Money(0, "USD")
    assert empty.lines == ()


def test_prices_in_another_currency_or_for_skus_not_asked_are_ignored():
    a = FakeSource("a", {"x": Money(1, "USD"), "y": eur(20)})

    class Chatty(FakeSource):
        """Answers with a cheap y only in the batch that did not ask for y."""

        async def prices(self, skus):
            answer = await super().prices(skus)
            return answer if "y" in skus else {**answer, "y": eur(1), "not-asked": eur(1)}

    b = Chatty("b", {"x": eur(30)})
    result = asyncio.run(quote({"x": 1, "y": 1}, [a, b], batch_size=1))
    assert [(line.sku, line.unit, line.source) for line in result.lines] == [
        ("x", eur(30), "b"),
        ("y", eur(20), "a"),
    ]


def test_each_source_is_asked_once_per_batch_of_sorted_skus():
    skus = [f"sku-{n:03}" for n in range(120)]
    a = FakeSource("a", {})
    b = FakeSource("b", {})
    asyncio.run(quote(dict.fromkeys(reversed(skus), 1), [a, b], batch_size=50))
    for source in (a, b):
        assert source.calls == [skus[0:50], skus[50:100], skus[100:120]]


def test_calls_run_concurrently_but_never_more_than_the_limit():
    tracker = Tracker()
    sources = [FakeSource(name, {}, delay=0.05, tracker=tracker) for name in "abc"]
    basket = {f"s{n}": 1 for n in range(4)}
    start = time.perf_counter()
    asyncio.run(quote(basket, sources, batch_size=2, limit=2))
    elapsed = time.perf_counter() - start
    assert tracker.max_active == 2
    assert elapsed < 0.25  # 6 calls of 0.05 s, two at a time: about 0.15 s


def test_a_slow_or_broken_source_is_reported_and_the_rest_still_count():
    good = FakeSource("good", {"x": eur(70)})
    slow = FakeSource("slow", {"x": eur(1)}, delay=1.0)
    broken = FakeSource("broken", {"x": eur(1)}, error=ConnectionError("down"))
    start = time.perf_counter()
    result = asyncio.run(quote({"x": 1}, [slow, broken, good], timeout=0.05))
    assert time.perf_counter() - start < 0.5
    assert result.failed == ("broken", "slow")
    assert result.lines == (Line("x", 1, eur(70), "good"),)


def test_a_source_is_listed_once_in_failed_even_if_several_batches_fail():
    broken = FakeSource("broken", {}, error=RuntimeError("bug"))
    result = asyncio.run(quote({"a": 1, "b": 1, "c": 1}, [broken], batch_size=1))
    assert result.failed == ("broken",)
    assert result.missing == frozenset({"a", "b", "c"})


def test_waiting_for_a_slot_does_not_count_against_the_timeout():
    tracker = Tracker()
    sources = [FakeSource(name, {"x": eur(5)}, delay=0.03, tracker=tracker) for name in "abcd"]
    result = asyncio.run(quote({"x": 1}, sources, limit=1, timeout=0.08))
    assert result.failed == ()
    assert tracker.max_active == 1


def test_cancelling_the_quote_cancels_the_calls_in_flight():
    tracker = Tracker()
    sources = [FakeSource(name, {}, delay=5.0, tracker=tracker) for name in "ab"]

    async def main():
        task = asyncio.create_task(quote({"x": 1}, sources))
        await asyncio.sleep(0.05)
        task.cancel()
        with pytest.raises(asyncio.CancelledError):
            await task

    start = time.perf_counter()
    asyncio.run(main())
    assert time.perf_counter() - start < 1.0
    assert tracker.cancelled == 2
    assert tracker.active == 0


def test_bad_arguments_fail_before_any_call():
    a = FakeSource("a", {})
    for basket in ({"x": 0}, {"x": -1}, {"x": 1.5}, {"x": True}):
        with pytest.raises(ValueError):
            asyncio.run(quote(basket, [a]))
    with pytest.raises(ValueError):
        asyncio.run(quote({"x": 1}, [a], batch_size=0))
    assert a.calls == []


def test_a_large_basket_is_fast():
    skus = [f"p{n:05}" for n in range(20_000)]
    a = FakeSource("a", {sku: eur(100 + n % 7) for n, sku in enumerate(skus)})
    b = FakeSource("b", {sku: eur(100 + n % 5) for n, sku in enumerate(skus)})
    start = time.perf_counter()
    result = asyncio.run(quote(dict.fromkeys(skus, 2), [a, b], batch_size=500, limit=8))
    elapsed = time.perf_counter() - start
    assert len(result.lines) == 20_000
    assert len(a.calls) == 40
    assert elapsed < 1.5
