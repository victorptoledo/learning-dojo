"""HTTP API: POST /orders stores an order and queues an event; GET /orders/{id} reads one.

Given code: the kata is the template, not this module.
"""

import json
import os
import uuid

import boto3

dynamodb = boto3.client("dynamodb")
sqs = boto3.client("sqs")


def handler(event, context):
    method = event["requestContext"]["http"]["method"]
    if method == "POST":
        order = json.loads(event.get("body") or "{}")
        order_id = str(uuid.uuid4())
        dynamodb.put_item(
            TableName=os.environ["TABLE_NAME"],
            Item={"pk": {"S": f"ORDER#{order_id}"}, "sk": {"S": "ORDER"},
                  "body": {"S": json.dumps(order)}, "status": {"S": "PLACED"}},
            ConditionExpression="attribute_not_exists(pk)",
        )
        sqs.send_message(QueueUrl=os.environ["QUEUE_URL"],
                         MessageBody=json.dumps({"order_id": order_id, "type": "OrderPlaced"}))
        return {"statusCode": 201, "body": json.dumps({"order_id": order_id})}
    order_id = event["pathParameters"]["id"]
    found = dynamodb.get_item(TableName=os.environ["TABLE_NAME"],
                              Key={"pk": {"S": f"ORDER#{order_id}"}, "sk": {"S": "ORDER"}})
    if "Item" not in found:
        return {"statusCode": 404, "body": json.dumps({"error": "not found"})}
    item = found["Item"]
    return {"statusCode": 200,
            "body": json.dumps({"order_id": order_id, "status": item["status"]["S"],
                                "order": json.loads(item["body"]["S"])})}
