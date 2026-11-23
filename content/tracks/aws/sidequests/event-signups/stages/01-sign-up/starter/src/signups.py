"""The sign-ups API: one Lambda handler behind an API Gateway HTTP API (payload 2.0)."""

import json
import os

import boto3

dynamodb = boto3.client("dynamodb")


def table() -> str:
    return os.environ["TABLE_NAME"]


def respond(status: int, body: dict | None = None) -> dict:
    response = {"statusCode": status, "headers": {"content-type": "application/json"}}
    if body is not None:
        response["body"] = json.dumps(body)
    return response


def handler(event, context):
    raise NotImplementedError(f"todo: answer {event.get('routeKey')}")
