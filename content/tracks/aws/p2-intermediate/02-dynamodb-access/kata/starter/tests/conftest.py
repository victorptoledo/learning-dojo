"""A fresh Orders table in moto (an in-memory DynamoDB) for every test."""

import boto3
import pytest
from moto import mock_aws

TABLE = "Orders"


@pytest.fixture
def client():
    with mock_aws():
        dynamodb = boto3.client("dynamodb", region_name="eu-west-1")
        dynamodb.create_table(
            TableName=TABLE,
            BillingMode="PAY_PER_REQUEST",
            KeySchema=[{"AttributeName": "pk", "KeyType": "HASH"},
                       {"AttributeName": "sk", "KeyType": "RANGE"}],
            AttributeDefinitions=[{"AttributeName": name, "AttributeType": "S"}
                                  for name in ("pk", "sk", "gsi1pk", "gsi1sk")],
            GlobalSecondaryIndexes=[{
                "IndexName": "by-status",
                "KeySchema": [{"AttributeName": "gsi1pk", "KeyType": "HASH"},
                              {"AttributeName": "gsi1sk", "KeyType": "RANGE"}],
                "Projection": {"ProjectionType": "ALL"},
            }],
        )
        yield dynamodb


@pytest.fixture
def repo(client):
    from orders import OrderRepository

    return OrderRepository(TABLE, client)
