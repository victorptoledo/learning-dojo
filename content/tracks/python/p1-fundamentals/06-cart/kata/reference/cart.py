"""A shopping cart built from dataclasses."""

from dataclasses import dataclass, field
from decimal import Decimal, InvalidOperation
from enum import StrEnum, auto

CENT = Decimal("0.01")


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
        sku = self.sku.strip().upper()
        if not sku:
            raise ValueError("a product needs a sku")
        if isinstance(self.price, float):
            raise TypeError("use Decimal, str or int for prices, not float")
        try:
            price = Decimal(self.price)
        except InvalidOperation as error:
            raise ValueError(f"invalid price: {self.price!r}") from error
        if not price.is_finite() or price < 0 or price != price.quantize(CENT):
            raise ValueError(f"invalid price: {self.price!r}")
        object.__setattr__(self, "sku", sku)
        object.__setattr__(self, "price", price.quantize(CENT))


@dataclass(slots=True)
class LineItem:
    product: Product
    quantity: int

    @property
    def total(self) -> Decimal:
        return self.product.price * self.quantity


@dataclass
class Cart:
    lines: list[LineItem] = field(default_factory=list)

    def add(self, product: Product, quantity: int = 1) -> None:
        if quantity < 1:
            raise ValueError("quantity must be at least 1")
        line = self._line(product.sku)
        if line is None:
            self.lines.append(LineItem(product, quantity))
        elif line.product != product:
            raise ValueError(f"another product already uses sku {product.sku}")
        else:
            line.quantity += quantity

    def remove(self, sku: str, quantity: int | None = None) -> None:
        line = self._line(sku.strip().upper())
        if line is None:
            raise KeyError(sku)
        if quantity is None or quantity >= line.quantity:
            self.lines.remove(line)
        elif quantity < 1:
            raise ValueError("quantity must be at least 1")
        else:
            line.quantity -= quantity

    @property
    def total(self) -> Decimal:
        return sum((line.total for line in self.lines), Decimal("0.00"))

    def total_for(self, category: Category) -> Decimal:
        return sum(
            (line.total for line in self.lines if line.product.category == category),
            Decimal("0.00"),
        )

    def receipt(self) -> str:
        rows = [
            f"{line.quantity} x {line.product.name} ({line.product.sku}) "
            f"@ {line.product.price} = {line.total}"
            for line in self.lines
        ]
        rows.append(f"TOTAL {self.total}")
        return "\n".join(rows)

    def __len__(self) -> int:
        return sum(line.quantity for line in self.lines)

    def __contains__(self, sku: object) -> bool:
        return isinstance(sku, str) and self._line(sku.strip().upper()) is not None

    def _line(self, sku: str) -> LineItem | None:
        return next((line for line in self.lines if line.product.sku == sku), None)
