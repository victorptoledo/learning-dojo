"""The sign-ups API: one Lambda handler behind an API Gateway HTTP API (payload 2.0)."""

import json
import os
import re

import boto3
from botocore.exceptions import ClientError

dynamodb = boto3.client("dynamodb")

EVENT_ID = re.compile(r"[a-z0-9-]{3,40}")
EMAIL = re.compile(r"[^@\s]+@[^@\s]+\.[^@\s]+")


class Problem(Exception):
    def __init__(self, status: int, message: str):
        super().__init__(message)
        self.status = status
        self.message = message


def table() -> str:
    return os.environ["TABLE_NAME"]


def respond(status: int, body: dict | None = None) -> dict:
    response = {"statusCode": status, "headers": {"content-type": "application/json"}}
    if body is not None:
        response["body"] = json.dumps(body)
    return response


def event_key(event_id: str) -> dict:
    return {"pk": {"S": f"EVENT#{event_id}"}, "sk": {"S": "META"}}


def signup_key(event_id: str, email: str) -> dict:
    return {"pk": {"S": f"EVENT#{event_id}"}, "sk": {"S": f"SIGNUP#{email}"}}


def read_body(request) -> dict:
    text = request.get("body") or ""
    try:
        body = json.loads(text)
    except ValueError as error:
        raise Problem(400, "the body is not JSON") from error
    if not isinstance(body, dict):
        raise Problem(400, "the body must be a JSON object")
    return body


def normalized_email(value) -> str:
    if not isinstance(value, str) or not EMAIL.fullmatch(value.strip()):
        raise Problem(400, "email must look like name@domain.tld")
    return value.strip().lower()


def failed_conditions(error: ClientError) -> list[bool]:
    reasons = error.response.get("CancellationReasons", [])
    return [reason.get("Code") == "ConditionalCheckFailed" for reason in reasons]


# ---- events --------------------------------------------------------------------------

def create_event(request) -> dict:
    body = read_body(request)
    event_id, title, capacity = body.get("id"), body.get("title"), body.get("capacity")
    if not isinstance(event_id, str) or not EVENT_ID.fullmatch(event_id):
        raise Problem(400, "id must be 3 to 40 characters of a-z, 0-9 and -")
    if not isinstance(title, str) or not title.strip() or len(title) > 200:
        raise Problem(400, "title must be a non-empty string of at most 200 characters")
    if isinstance(capacity, bool) or not isinstance(capacity, int) or not 1 <= capacity <= 10_000:
        raise Problem(400, "capacity must be an integer from 1 to 10000")
    try:
        dynamodb.put_item(
            TableName=table(),
            Item={**event_key(event_id), "title": {"S": title}, "capacity": {"N": str(capacity)},
                  "taken": {"N": "0"}},
            ConditionExpression="attribute_not_exists(pk)",
        )
    except ClientError as error:
        if error.response["Error"]["Code"] == "ConditionalCheckFailedException":
            raise Problem(409, "event exists") from error
        raise
    return respond(201, {"id": event_id, "title": title, "capacity": capacity, "taken": 0,
                         "remaining": capacity})


def load_event(event_id: str) -> dict:
    item = dynamodb.get_item(TableName=table(), Key=event_key(event_id), ConsistentRead=True).get("Item")
    if item is None:
        raise Problem(404, "no such event")
    capacity, taken = int(item["capacity"]["N"]), int(item["taken"]["N"])
    return {"id": event_id, "title": item["title"]["S"], "capacity": capacity, "taken": taken,
            "remaining": capacity - taken}


def get_event(request) -> dict:
    return respond(200, load_event(request["pathParameters"]["id"]))


# ---- sign-ups ------------------------------------------------------------------------

def sign_up(request) -> dict:
    event_id = request["pathParameters"]["id"]
    email = normalized_email(read_body(request).get("email"))
    result = {"event": event_id, "email": email}
    operations = [
        {"Update": {
            "TableName": table(),
            "Key": event_key(event_id),
            "UpdateExpression": "SET taken = taken + :one",
            "ConditionExpression": "attribute_exists(pk) AND taken < #capacity",
            "ExpressionAttributeNames": {"#capacity": "capacity"},
            "ExpressionAttributeValues": {":one": {"N": "1"}},
        }},
        {"Put": {
            "TableName": table(),
            "Item": {**signup_key(event_id, email), "email": {"S": email}},
            "ConditionExpression": "attribute_not_exists(pk)",
        }},
    ]
    try:
        dynamodb.transact_write_items(TransactItems=operations)
    except ClientError as error:
        if error.response["Error"]["Code"] != "TransactionCanceledException":
            raise
        failed = failed_conditions(error)
        if len(failed) > 1 and failed[1]:
            raise Problem(409, "already signed up") from error
        if failed and failed[0]:
            load_event(event_id)  # 404 when there is no such event
            raise Problem(409, "event is full") from error
        raise
    return respond(201, result)


ROUTES = {
    "POST /events": create_event,
    "GET /events/{id}": get_event,
    "POST /events/{id}/signups": sign_up,
}


def handler(event, context):
    route = ROUTES.get(event.get("routeKey", ""))
    if route is None:
        return respond(404, {"error": "no such route"})
    try:
        return route(event)
    except Problem as problem:
        return respond(problem.status, {"error": problem.message})
