from decimal import Decimal

from cart import Cart, Category, Product


def test_totals_the_cart():
    cart = Cart()
    cart.add(Product("TEA-01", "Green tea", Decimal("3.50"), Category.FOOD), 2)
    assert cart.total == Decimal("7.00")
