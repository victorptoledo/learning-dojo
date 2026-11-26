"""Quote a basket against several price sources: a money type, a protocol and async fan-out."""

from collections.abc import Mapping, Sequence
from dataclasses import dataclass
from functools import total_ordering
from typing import Protocol, Self


class CurrencyMismatch(ValueError):
    """Two amounts in different currencies were combined or compared."""


@total_ordering
@dataclass(frozen=True, slots=True)
class Money:
    """An amount in cents. The dataclass already gives you __init__, __eq__ and __hash__."""

    cents: int
    currency: str = "EUR"

    def __add__(self, other: object) -> Self:
        raise NotImplementedError("todo: Money + Money")

    def __radd__(self, other: object) -> Self:
        raise NotImplementedError("todo: make sum() work")

    def __mul__(self, times: object) -> Self:
        raise NotImplementedError("todo: Money * int")

    def __rmul__(self, times: object) -> Self:
        raise NotImplementedError("todo: int * Money")

    def __lt__(self, other: object) -> bool:
        raise NotImplementedError("todo: ordering (total_ordering adds the rest)")

    def __format__(self, spec: str) -> str:
        raise NotImplementedError("todo: f'{money}' and f'{money:,.1f}'")

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
    raise NotImplementedError("todo: write quote()")
