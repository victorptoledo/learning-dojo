"""Orders in one DynamoDB table (single-table design), with boto3.

Item layout (see the README):
    pk = "CUSTOMER#<customer_id>"   sk = "ORDER#<order_id>"
    gsi1pk = "STATUS#<status>"      gsi1sk = "<order_id>"     (index "by-status")
    customer_id, order_id, status, total (Decimal), items (list of str)
"""

from decimal import Decimal
from typing import Any

import boto3

STATUS_INDEX = "by-status"


class OrderExists(Exception):
    """put_order was called for an order that is already stored."""


class InvalidTransition(Exception):
    """The order is missing or not in the status the change needs."""


class OrderRepository:
    def __init__(self, table_name: str, client: Any = None):
        self.table_name = table_name
        self.client = client or boto3.client("dynamodb")

    def put_order(self, order: dict) -> None:
        """Stores a new order; raises OrderExists if this customer already has that order id."""
        raise NotImplementedError("todo: put_item with a condition so an order is never overwritten")

    def get_order(self, customer_id: str, order_id: str) -> dict | None:
        """The order as a plain dict (no key attributes), or None."""
        raise NotImplementedError("todo: get_item on the order's key")

    def orders_for_customer(self, customer_id: str, page_size: int = 25) -> list[dict]:
        """Every order of the customer, newest order id first, following every page."""
        raise NotImplementedError("todo: query the customer's partition, newest first, all pages")

    def orders_by_status(self, status: str) -> list[dict]:
        """Every order in a status, oldest order id first, from the by-status index."""
        raise NotImplementedError("todo: query the by-status index")

    def mark_shipped(self, customer_id: str, order_id: str) -> None:
        """PLACED -> SHIPPED; raises InvalidTransition if the order is missing or not PLACED."""
        raise NotImplementedError("todo: update_item with a condition on the current status")


# Helpers you may use: DynamoDB's typed JSON (`{"S": "x"}`, `{"N": "12.50"}`) to and from Python.
def to_item(values: dict) -> dict:
    item = {}
    for name, value in values.items():
        if isinstance(value, str):
            item[name] = {"S": value}
        elif isinstance(value, (int, Decimal)):
            item[name] = {"N": str(value)}
        elif isinstance(value, list):
            item[name] = {"L": [{"S": str(entry)} for entry in value]}
    return item


def from_item(item: dict) -> dict:
    values: dict = {}
    for name, typed in item.items():
        if "S" in typed:
            values[name] = typed["S"]
        elif "N" in typed:
            values[name] = Decimal(typed["N"])
        elif "L" in typed:
            values[name] = [entry["S"] for entry in typed["L"]]
    return values

