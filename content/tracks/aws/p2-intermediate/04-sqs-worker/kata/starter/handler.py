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

# Created once per execution environment and reused by later invocations (warm starts).
_dynamodb = None


def dynamodb() -> Any:
    global _dynamodb
    if _dynamodb is None:
        _dynamodb = boto3.client("dynamodb")
    return _dynamodb


def parse_payment(body: str) -> dict:
    """The payment in a message body; ValueError if it is not valid."""
    raise NotImplementedError("todo: parse and validate the JSON body")


def apply_payment(payment: dict) -> None:
    """Marks the payment applied and adds it to the balance, atomically and only once."""
    raise NotImplementedError("todo: one transact_write_items with a condition on the marker")


def handle(event: dict, context: Any) -> dict:
    """The SQS batch handler: reports only the messages that failed."""
    raise NotImplementedError("todo: process each record and return batchItemFailures")
