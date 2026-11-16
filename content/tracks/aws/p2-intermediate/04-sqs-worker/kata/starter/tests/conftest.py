"""A fresh Payments table in moto, and helpers to build SQS events like Lambda sends them."""

import json

import boto3
import pytest
from moto import mock_aws

TABLE = "Payments"


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setenv("TABLE_NAME", TABLE)
    with mock_aws():
        dynamodb = boto3.client("dynamodb", region_name="eu-west-1")
        dynamodb.create_table(
            TableName=TABLE,
            BillingMode="PAY_PER_REQUEST",
            KeySchema=[{"AttributeName": "pk", "KeyType": "HASH"}],
            AttributeDefinitions=[{"AttributeName": "pk", "AttributeType": "S"}],
        )
        import handler

        monkeypatch.setattr(handler, "_dynamodb", dynamodb)
        yield dynamodb


def record(message_id, body):
    text = body if isinstance(body, str) else json.dumps(body)
    return {"messageId": message_id, "receiptHandle": f"rh-{message_id}", "body": text,
            "eventSource": "aws:sqs",
            "eventSourceARN": "arn:aws:sqs:eu-west-1:111122223333:payments",
            "attributes": {"ApproximateReceiveCount": "1"}}


def event(*records):
    return {"Records": list(records)}


def balance(client, account):
    item = client.get_item(TableName=TABLE, Key={"pk": {"S": f"ACCOUNT#{account}"}}).get("Item")
    return int(item["balance_cents"]["N"]) if item else 0
