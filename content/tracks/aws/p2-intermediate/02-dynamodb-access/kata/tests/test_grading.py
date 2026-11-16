from decimal import Decimal

import pytest

from orders import InvalidTransition, OrderExists


def order(customer="c-17", number=1, status="PLACED", total="10.00"):
    return {"customer_id": customer, "order_id": f"20261115-{number:04d}", "status": status,
            "total": Decimal(total), "items": [f"sku-{number}"]}


def raw_item(client, customer, order_id):
    return client.get_item(TableName="Orders", Key={
        "pk": {"S": f"CUSTOMER#{customer}"}, "sk": {"S": f"ORDER#{order_id}"}}).get("Item")


def test_stores_the_documented_key_layout(repo, client):
    repo.put_order(order(number=7))
    item = raw_item(client, "c-17", "20261115-0007")
    assert item is not None, "store the order at pk=CUSTOMER#<customer_id>, sk=ORDER#<order_id>"
    assert item["gsi1pk"] == {"S": "STATUS#PLACED"}, "set gsi1pk to STATUS#<status>"
    assert item["gsi1sk"] == {"S": "20261115-0007"}, "set gsi1sk to the order id"
    assert item["total"] == {"N": "10.00"}, "store total as a number"


def test_get_order_returns_none_for_an_unknown_order(repo):
    assert repo.get_order("c-17", "20261115-9999") is None


def test_get_order_hides_the_key_attributes(repo):
    repo.put_order(order())
    found = repo.get_order("c-17", "20261115-0001")
    assert not {"pk", "sk", "gsi1pk", "gsi1sk"} & set(found), "return only the order's own fields"


def test_never_overwrites_an_existing_order(repo):
    repo.put_order(order(total="10.00"))
    with pytest.raises(OrderExists):
        repo.put_order(order(total="99.00"))
    assert repo.get_order("c-17", "20261115-0001")["total"] == Decimal("10.00")


def test_the_same_order_id_may_exist_for_two_customers(repo):
    repo.put_order(order(customer="c-1"))
    repo.put_order(order(customer="c-2"))
    assert repo.get_order("c-2", "20261115-0001")["customer_id"] == "c-2"


def test_lists_a_customers_orders_newest_first(repo):
    for number in (3, 1, 2):
        repo.put_order(order(number=number))
    repo.put_order(order(customer="c-99", number=4))
    ids = [o["order_id"] for o in repo.orders_for_customer("c-17")]
    assert ids == ["20261115-0003", "20261115-0002", "20261115-0001"]


def test_follows_every_page_of_results(repo):
    for number in range(1, 38):
        repo.put_order(order(number=number))
    found = repo.orders_for_customer("c-17", page_size=5)
    assert len(found) == 37, f"expected 37 orders over 8 pages, got {len(found)}: follow LastEvaluatedKey"


def test_does_not_scan_the_table(repo, client, monkeypatch):
    repo.put_order(order())

    def no_scan(**_):
        raise AssertionError("use query, never scan: a scan reads the whole table")

    monkeypatch.setattr(client, "scan", no_scan)
    repo.orders_for_customer("c-17")
    repo.orders_by_status("PLACED")


def test_lists_orders_by_status_from_the_index(repo):
    repo.put_order(order(customer="c-1", number=2))
    repo.put_order(order(customer="c-2", number=1))
    repo.put_order(order(customer="c-3", number=3, status="CANCELLED"))
    ids = [o["order_id"] for o in repo.orders_by_status("PLACED")]
    assert ids == ["20261115-0001", "20261115-0002"]


def test_mark_shipped_moves_the_order_to_the_shipped_index(repo):
    repo.put_order(order())
    repo.mark_shipped("c-17", "20261115-0001")
    assert repo.get_order("c-17", "20261115-0001")["status"] == "SHIPPED"
    assert repo.orders_by_status("PLACED") == []
    assert [o["order_id"] for o in repo.orders_by_status("SHIPPED")] == ["20261115-0001"]


def test_mark_shipped_refuses_an_order_that_is_not_placed(repo):
    repo.put_order(order(status="CANCELLED"))
    with pytest.raises(InvalidTransition):
        repo.mark_shipped("c-17", "20261115-0001")
    assert repo.get_order("c-17", "20261115-0001")["status"] == "CANCELLED"


def test_mark_shipped_does_not_create_a_missing_order(repo):
    with pytest.raises(InvalidTransition):
        repo.mark_shipped("c-17", "20261115-0404")
    assert repo.get_order("c-17", "20261115-0404") is None, "update_item must not create an item"
