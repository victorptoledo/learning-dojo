"""Deterministic test data for the pipeline, and a timer."""

import random
import time
from contextlib import contextmanager

CURRENCIES = ["EUR", "USD", "BRL", "GBP"]


def orders(count: int, customers: int, products: int, seed: int = 7) -> list[dict]:
    rng = random.Random(seed)
    return [
        {
            "id": i,
            "customer": f"c{rng.randrange(customers)}",
            "product": f"p{rng.randrange(products)}",
            "quantity": rng.randint(1, 9),
            "price": rng.randint(100, 99999) / 100,
            "currency": rng.choice(CURRENCIES),
        }
        for i in range(count)
    ]


def customers(count: int, seed: int = 11) -> list[dict]:
    rng = random.Random(seed)
    return [{"id": f"c{i}", "country": rng.choice(["BR", "PT", "DE", "US", "JP"])} for i in range(count)]


@contextmanager
def budget(seconds: float, what: str):
    started = time.perf_counter()
    yield
    elapsed = time.perf_counter() - started
    assert elapsed < seconds, f"{what} took {elapsed:.2f} s; the budget is {seconds} s"
