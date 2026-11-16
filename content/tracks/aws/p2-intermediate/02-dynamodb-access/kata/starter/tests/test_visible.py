from decimal import Decimal


def test_an_order_can_be_stored_and_read_back(repo):
    order = {"customer_id": "c-17", "order_id": "20261115-0001", "status": "PLACED",
             "total": Decimal("42.50"), "items": ["sku-1", "sku-2"]}
    repo.put_order(order)
    assert repo.get_order("c-17", "20261115-0001") == order
