import asyncio

from quotes import Line, Money, quote


class Shop:
    def __init__(self, name, price_list):
        self.name = name
        self.price_list = price_list

    async def prices(self, skus):
        await asyncio.sleep(0)
        return {sku: self.price_list[sku] for sku in skus if sku in self.price_list}


def test_the_cheapest_shop_wins_each_line():
    corner = Shop("corner", {"milk": Money(120), "bread": Money(250)})
    market = Shop("market", {"milk": Money(99)})
    result = asyncio.run(quote({"milk": 2, "bread": 1}, [corner, market]))
    assert result.lines == (Line("bread", 1, Money(250), "corner"), Line("milk", 2, Money(99), "market"))
    assert f"{result.total}" == "4.48 EUR"
