import dataclasses
from decimal import Decimal

import pytest

from cart import Cart, Category, LineItem, Product


def tea():
    return Product("tea-01", "Green tea", "3.5", Category.FOOD)


def dune():
    return Product("BK-7", "Dune", Decimal("9.99"), Category.BOOKS)


def test_product_normalises_sku_and_price():
    product = Product(" tea-01 ", "Green tea", "3.5", Category.FOOD)
    assert product.sku == "TEA-01"
    assert product.price == Decimal("3.50")
    assert str(product.price) == "3.50"
    assert str(Product("X", "x", 4).price) == "4.00"


def test_product_is_an_immutable_hashable_value():
    product = tea()
    with pytest.raises(dataclasses.FrozenInstanceError):
        product.price = Decimal("1.00")
    assert product == tea()
    assert len({tea(), tea(), dune()}) == 2
    assert Product("x", "x", "1").category is Category.OTHER


def test_product_rejects_a_blank_sku():
    with pytest.raises(ValueError):
        Product("   ", "Nothing", "1.00")


def test_product_rejects_float_prices():
    with pytest.raises(TypeError):
        Product("X", "x", 3.5)


@pytest.mark.parametrize("price", ["-0.01", "1.005", "abc", Decimal("-2")])
def test_product_rejects_invalid_prices(price):
    with pytest.raises(ValueError):
        Product("X", "x", price)


def test_line_item_total():
    assert LineItem(tea(), 3).total == Decimal("10.50")


def test_add_merges_the_same_product_and_keeps_order():
    cart = Cart()
    cart.add(tea())
    cart.add(dune())
    cart.add(tea(), 2)
    assert [(line.product.sku, line.quantity) for line in cart.lines] == [("TEA-01", 3), ("BK-7", 1)]
    assert len(cart) == 4


def test_add_rejects_bad_quantities_and_conflicting_skus():
    cart = Cart()
    with pytest.raises(ValueError):
        cart.add(tea(), 0)
    cart.add(tea())
    with pytest.raises(ValueError):
        cart.add(Product("TEA-01", "Black tea", "2.00"))
    assert len(cart) == 1


def test_carts_do_not_share_lines():
    first, second = Cart(), Cart()
    first.add(tea())
    assert second.lines == []
    assert len(second) == 0


def test_remove_decreases_or_removes_the_line():
    cart = Cart()
    cart.add(tea(), 3)
    cart.add(dune())
    cart.remove("tea-01", 2)
    assert len(cart) == 2
    cart.remove("TEA-01", 5)
    assert "TEA-01" not in cart
    cart.remove("bk-7")
    assert cart.lines == []


def test_remove_errors():
    cart = Cart()
    cart.add(tea())
    with pytest.raises(KeyError):
        cart.remove("NOPE")
    with pytest.raises(ValueError):
        cart.remove("TEA-01", 0)


def test_totals():
    cart = Cart()
    assert cart.total == Decimal("0.00")
    cart.add(tea(), 2)
    cart.add(dune())
    assert cart.total == Decimal("16.99")
    assert cart.total_for(Category.BOOKS) == Decimal("9.99")
    assert cart.total_for(Category.OTHER) == Decimal("0.00")


def test_contains_normalises_the_sku():
    cart = Cart()
    cart.add(tea())
    assert " tea-01" in cart
    assert "BK-7" not in cart


def test_receipt():
    cart = Cart()
    cart.add(tea(), 2)
    cart.add(dune())
    assert cart.receipt() == (
        "2 x Green tea (TEA-01) @ 3.50 = 7.00\n"
        "1 x Dune (BK-7) @ 9.99 = 9.99\n"
        "TOTAL 16.99"
    )
    assert Cart().receipt() == "TOTAL 0.00"
