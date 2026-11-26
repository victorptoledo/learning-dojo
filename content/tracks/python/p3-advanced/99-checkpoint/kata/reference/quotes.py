"""Quote a basket against several price sources: a money type, a protocol and async fan-out."""

import asyncio
from collections.abc import Mapping, Sequence
from dataclasses import dataclass
from decimal import Decimal
from functools import total_ordering
from typing import Protocol, Self


class CurrencyMismatch(ValueError):
    """Two amounts in different currencies were combined or compared."""


@total_ordering
@dataclass(frozen=True, slots=True)
class Money:
    cents: int
    currency: str = "EUR"

    def _check(self, other: Self) -> None:
        if other.currency != self.currency:
            raise CurrencyMismatch(f"{self.currency} and {other.currency}")

    def __add__(self, other: object) -> Self:
        if not isinstance(other, Money):
            return NotImplemented
        self._check(other)
        return Money(self.cents + other.cents, self.currency)

    def __radd__(self, other: object) -> Self:
        # sum() starts from the int 0
        if isinstance(other, int) and not isinstance(other, bool) and other == 0:
            return self
        return NotImplemented

    def __mul__(self, times: object) -> Self:
        if not isinstance(times, int) or isinstance(times, bool):
            return NotImplemented
        return Money(self.cents * times, self.currency)

    __rmul__ = __mul__

    def __lt__(self, other: object) -> bool:
        if not isinstance(other, Money):
            return NotImplemented
        self._check(other)
        return self.cents < other.cents

    def __format__(self, spec: str) -> str:
        amount = Decimal(self.cents).scaleb(-2)
        return f"{format(amount, spec or '.2f')} {self.currency}"

    def __str__(self) -> str:
        return format(self, "")


class PriceSource(Protocol):
    """Anything with a name and a batch price lookup is a source; no base class needed."""

    name: str

    async def prices(self, skus: Sequence[str]) -> Mapping[str, Money]: ...


@dataclass(frozen=True, slots=True)
class Line:
    sku: str
    quantity: int
    unit: Money
    source: str

    @property
    def subtotal(self) -> Money:
        return self.unit * self.quantity


@dataclass(frozen=True, slots=True)
class Quote:
    total: Money
    lines: tuple[Line, ...]
    missing: frozenset[str]
    failed: tuple[str, ...]


async def quote(
    basket: Mapping[str, int],
    sources: Sequence[PriceSource],
    *,
    currency: str = "EUR",
    batch_size: int = 50,
    limit: int = 4,
    timeout: float = 1.0,
) -> Quote:
    """Price every SKU of the basket at its cheapest source, asking each source in batches."""
    if batch_size < 1 or limit < 1:
        raise ValueError("batch_size and limit must be at least 1")
    for sku, quantity in basket.items():
        if not isinstance(quantity, int) or isinstance(quantity, bool) or quantity < 1:
            raise ValueError(f"bad quantity for {sku!r}: {quantity!r}")

    skus = sorted(basket)
    chunks = [skus[i : i + batch_size] for i in range(0, len(skus), batch_size)]
    semaphore = asyncio.Semaphore(limit)
    failed: set[str] = set()

    async def ask(source: PriceSource, chunk: list[str]) -> dict[str, Money]:
        async with semaphore:
            try:
                async with asyncio.timeout(timeout):
                    answer = await source.prices(chunk)
            except Exception:  # TimeoutError included; CancelledError is not an Exception
                failed.add(source.name)
                return {}
        wanted = set(chunk)
        return {
            sku: price
            for sku, price in answer.items()
            if sku in wanted and isinstance(price, Money) and price.currency == currency
        }

    async with asyncio.TaskGroup() as group:
        tasks = [
            (index, group.create_task(ask(source, chunk)))
            for index, source in enumerate(sources)
            for chunk in chunks
        ]

    best: dict[str, tuple[Money, int]] = {}
    for index, task in tasks:
        for sku, price in task.result().items():
            current = best.get(sku)
            if current is None or (price, index) < current:
                best[sku] = (price, index)

    lines = tuple(
        Line(sku, basket[sku], best[sku][0], sources[best[sku][1]].name)
        for sku in skus
        if sku in best
    )
    total = sum((line.subtotal for line in lines), Money(0, currency))
    return Quote(
        total=total,
        lines=lines,
        missing=frozenset(sku for sku in skus if sku not in best),
        failed=tuple(sorted(failed)),
    )
