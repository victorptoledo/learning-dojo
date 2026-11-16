"""A Lambda function that applies payments from an SQS queue to account balances.

Each SQS message body is JSON: {"payment_id": "p-1", "account": "acc-7", "amount_cents": 1250}.
The table (its name in the TABLE_NAME environment variable) holds two kinds of items:
    pk = "PAYMENT#<payment_id>"   a marker: this payment was applied
    pk = "ACCOUNT#<account>"      balance_cents, a number
"""

import json
import os
from typing import Any

import boto3
from botocore.exceptions import ClientError

# Created once per execution environment and reused by later invocations (warm starts).
_dynamodb = None


def dynamodb() -> Any:
    global _dynamodb
    if _dynamodb is None:
        _dynamodb = boto3.client("dynamodb")
    return _dynamodb


def parse_payment(body: str) -> dict:
    """The payment in a message body; ValueError if it is not valid."""
    try:
        data = json.loads(body)
    except json.JSONDecodeError as error:
        raise ValueError(f"not JSON: {error}") from error
    if not isinstance(data, dict):
        raise ValueError("the body must be a JSON object")
    payment_id, account, amount = data.get("payment_id"), data.get("account"), data.get("amount_cents")
    if not isinstance(payment_id, str) or not payment_id:
        raise ValueError("payment_id must be a non-empty string")
    if not isinstance(account, str) or not account:
        raise ValueError("account must be a non-empty string")
    if isinstance(amount, bool) or not isinstance(amount, int) or amount <= 0:
        raise ValueError("amount_cents must be a positive integer")
    return {"payment_id": payment_id, "account": account, "amount_cents": amount}


def apply_payment(payment: dict) -> None:
    """Marks the payment applied and adds it to the balance, atomically and only once."""
    table = os.environ["TABLE_NAME"]
    try:
        dynamodb().transact_write_items(TransactItems=[
            {"Put": {
                "TableName": table,
                "Item": {"pk": {"S": f"PAYMENT#{payment['payment_id']}"},
                         "account": {"S": payment["account"]},
                         "amount_cents": {"N": str(payment["amount_cents"])}},
                "ConditionExpression": "attribute_not_exists(pk)",
            }},
            {"Update": {
                "TableName": table,
                "Key": {"pk": {"S": f"ACCOUNT#{payment['account']}"}},
                "UpdateExpression": "ADD balance_cents :amount",
                "ExpressionAttributeValues": {":amount": {"N": str(payment["amount_cents"])}},
            }},
        ])
    except ClientError as error:
        reasons = error.response.get("CancellationReasons") or []
        if reasons and reasons[0].get("Code") == "ConditionalCheckFailed":
            return  # already applied: a duplicate delivery, nothing to do
        raise


def handle(event: dict, context: Any) -> dict:
    """The SQS batch handler: reports only the messages that failed."""
    failures = []
    for record in event.get("Records", []):
        try:
            apply_payment(parse_payment(record["body"]))
        except (ValueError, ClientError) as error:
            print(json.dumps({"level": "ERROR", "messageId": record["messageId"], "error": str(error)}))
            failures.append({"itemIdentifier": record["messageId"]})
    return {"batchItemFailures": failures}
