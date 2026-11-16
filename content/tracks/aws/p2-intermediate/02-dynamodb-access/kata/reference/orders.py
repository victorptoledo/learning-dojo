"""Orders in one DynamoDB table (single-table design), with boto3.

Item layout (see the README):
    pk = "CUSTOMER#<customer_id>"   sk = "ORDER#<order_id>"
    gsi1pk = "STATUS#<status>"      gsi1sk = "<order_id>"     (index "by-status")
    customer_id, order_id, status, total (Decimal), items (list of str)
"""

from decimal import Decimal
from typing import Any

import boto3
from botocore.exceptions import ClientError

STATUS_INDEX = "by-status"
KEY_ATTRIBUTES = ("pk", "sk", "gsi1pk", "gsi1sk")


class OrderExists(Exception):
    """put_order was called for an order that is already stored."""


class InvalidTransition(Exception):
    """The order is missing or not in the status the change needs."""


def _key(customer_id: str, order_id: str) -> dict:
    return {"pk": {"S": f"CUSTOMER#{customer_id}"}, "sk": {"S": f"ORDER#{order_id}"}}


def _failed_condition(error: ClientError) -> bool:
    return error.response["Error"]["Code"] == "ConditionalCheckFailedException"


class OrderRepository:
    def __init__(self, table_name: str, client: Any = None):
        self.table_name = table_name
        self.client = client or boto3.client("dynamodb")

    def put_order(self, order: dict) -> None:
        """Stores a new order; raises OrderExists if this customer already has that order id."""
        item = to_item({
            **order,
            "gsi1pk": f"STATUS#{order['status']}",
            "gsi1sk": order["order_id"],
        }) | _key(order["customer_id"], order["order_id"])
        try:
            self.client.put_item(
                TableName=self.table_name,
                Item=item,
                ConditionExpression="attribute_not_exists(pk)",
            )
        except ClientError as error:
            if _failed_condition(error):
                raise OrderExists(order["order_id"]) from error
            raise

    def get_order(self, customer_id: str, order_id: str) -> dict | None:
        """The order as a plain dict (no key attributes), or None."""
        found = self.client.get_item(TableName=self.table_name, Key=_key(customer_id, order_id))
        return _order(found["Item"]) if "Item" in found else None

    def orders_for_customer(self, customer_id: str, page_size: int = 25) -> list[dict]:
        """Every order of the customer, newest order id first, following every page."""
        return self._query(
            KeyConditionExpression="pk = :pk AND begins_with(sk, :order)",
            ExpressionAttributeValues={":pk": {"S": f"CUSTOMER#{customer_id}"},
                                       ":order": {"S": "ORDER#"}},
            ScanIndexForward=False,
            Limit=page_size,
        )

    def orders_by_status(self, status: str) -> list[dict]:
        """Every order in a status, oldest order id first, from the by-status index."""
        return self._query(
            IndexName=STATUS_INDEX,
            KeyConditionExpression="gsi1pk = :status",
            ExpressionAttributeValues={":status": {"S": f"STATUS#{status}"}},
        )

    def mark_shipped(self, customer_id: str, order_id: str) -> None:
        """PLACED -> SHIPPED; raises InvalidTransition if the order is missing or not PLACED."""
        try:
            self.client.update_item(
                TableName=self.table_name,
                Key=_key(customer_id, order_id),
                UpdateExpression="SET #status = :shipped, gsi1pk = :index",
                ConditionExpression="attribute_exists(pk) AND #status = :placed",
                ExpressionAttributeNames={"#status": "status"},
                ExpressionAttributeValues={":shipped": {"S": "SHIPPED"},
                                           ":placed": {"S": "PLACED"},
                                           ":index": {"S": "STATUS#SHIPPED"}},
            )
        except ClientError as error:
            if _failed_condition(error):
                raise InvalidTransition(f"{customer_id}/{order_id} is not PLACED") from error
            raise

    def _query(self, **request: Any) -> list[dict]:
        orders: list[dict] = []
        while True:
            page = self.client.query(TableName=self.table_name, **request)
            orders.extend(_order(item) for item in page["Items"])
            if "LastEvaluatedKey" not in page:
                return orders
            request["ExclusiveStartKey"] = page["LastEvaluatedKey"]


def _order(item: dict) -> dict:
    return {name: value for name, value in from_item(item).items() if name not in KEY_ATTRIBUTES}


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
