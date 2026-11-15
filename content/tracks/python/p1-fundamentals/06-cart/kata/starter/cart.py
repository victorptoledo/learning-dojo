"""A shopping cart built from dataclasses."""

from dataclasses import dataclass, field
from decimal import Decimal
from enum import StrEnum, auto


class Category(StrEnum):
    FOOD = auto()
    BOOKS = auto()
    OTHER = auto()


@dataclass(frozen=True, slots=True)
class Product:
    sku: str
    name: str
    price: Decimal
    category: Category = Category.OTHER

    def __post_init__(self) -> None:
        raise NotImplementedError("todo: validate and normalise sku and price")


@dataclass(slots=True)
class LineItem:
    product: Product
    quantity: int

    @property
    def total(self) -> Decimal:
        raise NotImplementedError("todo: write LineItem.total")


@dataclass
class Cart:
    lines: list[LineItem] = field(default_factory=list)

    def add(self, product: Product, quantity: int = 1) -> None:
        raise NotImplementedError("todo: write Cart.add()")

    def remove(self, sku: str, quantity: int | None = None) -> None:
        raise NotImplementedError("todo: write Cart.remove()")

    @property
    def total(self) -> Decimal:
        raise NotImplementedError("todo: write Cart.total")

    def total_for(self, category: Category) -> Decimal:
        raise NotImplementedError("todo: write Cart.total_for()")

    def receipt(self) -> str:
        raise NotImplementedError("todo: write Cart.receipt()")

    def __len__(self) -> int:
        raise NotImplementedError("todo: write len(cart)")

    def __contains__(self, sku: object) -> bool:
        raise NotImplementedError("todo: write sku in cart")
