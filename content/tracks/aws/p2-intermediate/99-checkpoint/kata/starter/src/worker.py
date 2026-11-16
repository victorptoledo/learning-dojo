"""Queue worker: confirms each placed order; reports only the messages that failed.

Given code: the kata is the template, not this module.
"""

import json
import os

import boto3
from botocore.exceptions import ClientError

dynamodb = boto3.client("dynamodb")


def handler(event, context):
    failures = []
    for record in event["Records"]:
        try:
            message = json.loads(record["body"])
            dynamodb.update_item(
                TableName=os.environ["TABLE_NAME"],
                Key={"pk": {"S": f"ORDER#{message['order_id']}"}, "sk": {"S": "ORDER"}},
                UpdateExpression="SET #status = :confirmed",
                ConditionExpression="#status = :placed",
                ExpressionAttributeNames={"#status": "status"},
                ExpressionAttributeValues={":confirmed": {"S": "CONFIRMED"},
                                           ":placed": {"S": "PLACED"}},
            )
        except ClientError as error:
            if error.response["Error"]["Code"] != "ConditionalCheckFailedException":
                failures.append({"itemIdentifier": record["messageId"]})
        except (KeyError, ValueError):
            failures.append({"itemIdentifier": record["messageId"]})
    return {"batchItemFailures": failures}
