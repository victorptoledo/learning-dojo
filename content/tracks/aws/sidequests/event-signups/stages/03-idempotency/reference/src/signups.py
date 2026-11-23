"""The sign-ups API: one Lambda handler behind an API Gateway HTTP API (payload 2.0)."""

import base64
import json
import os
import re
import time

import boto3
from botocore.exceptions import ClientError

dynamodb = boto3.client("dynamodb")

EVENT_ID = re.compile(r"[a-z0-9-]{3,40}")
EMAIL = re.compile(r"[^@\s]+@[^@\s]+\.[^@\s]+")
DEFAULT_LIMIT, MAX_LIMIT = 50, 100
IDEMPOTENCY_KEY = re.compile(r"[A-Za-z0-9_-]{1,64}")
KEEP_RESULTS = 24 * 60 * 60


class Problem(Exception):
    def __init__(self, status: int, message: str):
        super().__init__(message)
        self.status = status
        self.message = message


def table() -> str:
    return os.environ["TABLE_NAME"]


def now() -> float:
    return time.time()


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
    if request.get("isBase64Encoded"):
        text = base64.b64decode(text).decode("utf-8")
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
    key = idempotency_key(request)
    fingerprint = f"{event_id}\n{email}"
    if key is not None:
        stored = stored_result(key, fingerprint)
        if stored is not None:
            return stored
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
    if key is not None:
        operations.append({"Put": {
            "TableName": table(),
            "Item": {"pk": {"S": f"IDEMPOTENCY#{key}"}, "sk": {"S": "KEY"},
                     "fingerprint": {"S": fingerprint}, "status": {"N": "201"},
                     "result": {"S": json.dumps(result)},
                     "expires_at": {"N": str(int(now()) + KEEP_RESULTS)}},
            "ConditionExpression": "attribute_not_exists(pk) OR expires_at <= :now",
            "ExpressionAttributeValues": {":now": {"N": str(int(now()))}},
        }})
    try:
        dynamodb.transact_write_items(TransactItems=operations)
    except ClientError as error:
        if error.response["Error"]["Code"] != "TransactionCanceledException":
            raise
        failed = failed_conditions(error)
        if len(failed) > 2 and failed[2]:
            # Another request with the same key won the race: answer as it did.
            stored = stored_result(key, fingerprint)
            if stored is not None:
                return stored
        if len(failed) > 1 and failed[1]:
            raise Problem(409, "already signed up") from error
        if failed and failed[0]:
            load_event(event_id)  # 404 when there is no such event
            raise Problem(409, "event is full") from error
        raise
    return respond(201, result)


def cancel(request) -> dict:
    event_id = request["pathParameters"]["id"]
    email = request["pathParameters"]["email"].strip().lower()
    try:
        dynamodb.transact_write_items(TransactItems=[
            {"Delete": {
                "TableName": table(),
                "Key": signup_key(event_id, email),
                "ConditionExpression": "attribute_exists(pk)",
            }},
            {"Update": {
                "TableName": table(),
                "Key": event_key(event_id),
                "UpdateExpression": "SET taken = taken - :one",
                "ConditionExpression": "attribute_exists(pk) AND taken > :zero",
                "ExpressionAttributeValues": {":one": {"N": "1"}, ":zero": {"N": "0"}},
            }},
        ])
    except ClientError as error:
        if error.response["Error"]["Code"] == "TransactionCanceledException" and any(failed_conditions(error)):
            raise Problem(404, "not signed up") from error
        raise
    return {"statusCode": 204, "headers": {}}


def encode_cursor(email: str) -> str:
    return base64.urlsafe_b64encode(json.dumps({"after": email}).encode()).decode().rstrip("=")


def decode_cursor(text: str) -> str:
    try:
        decoded = json.loads(base64.urlsafe_b64decode(text + "=" * (-len(text) % 4)))
        after = decoded["after"]
    except (ValueError, KeyError, TypeError) as error:
        raise Problem(400, "cursor is not valid") from error
    if not isinstance(after, str):
        raise Problem(400, "cursor is not valid")
    return after


def list_signups(request) -> dict:
    event_id = request["pathParameters"]["id"]
    query = request.get("queryStringParameters") or {}
    try:
        limit = int(query.get("limit", DEFAULT_LIMIT))
    except ValueError as error:
        raise Problem(400, "limit must be a number") from error
    if not 1 <= limit <= MAX_LIMIT:
        raise Problem(400, f"limit must be from 1 to {MAX_LIMIT}")
    load_event(event_id)
    request_args = {
        "TableName": table(),
        "KeyConditionExpression": "pk = :pk AND begins_with(sk, :prefix)",
        "ExpressionAttributeValues": {":pk": {"S": f"EVENT#{event_id}"}, ":prefix": {"S": "SIGNUP#"}},
        "Limit": limit + 1,
    }
    if query.get("cursor"):
        request_args["ExclusiveStartKey"] = signup_key(event_id, decode_cursor(query["cursor"]))
    emails = []
    while len(emails) <= limit:
        page = dynamodb.query(**request_args)
        emails.extend(item["sk"]["S"].removeprefix("SIGNUP#") for item in page["Items"])
        if "LastEvaluatedKey" not in page:
            break
        request_args["ExclusiveStartKey"] = page["LastEvaluatedKey"]
    more = len(emails) > limit
    emails = emails[:limit]
    return respond(200, {"items": emails, "next": encode_cursor(emails[-1]) if more else None})


# ---- idempotency ---------------------------------------------------------------------

def idempotency_key(request) -> str | None:
    headers = {name.lower(): value for name, value in (request.get("headers") or {}).items()}
    key = headers.get("idempotency-key")
    if key is None:
        return None
    if not IDEMPOTENCY_KEY.fullmatch(key):
        raise Problem(400, "Idempotency-Key must be 1 to 64 characters of A-Z, a-z, 0-9, _ and -")
    return key


def stored_result(key: str, fingerprint: str) -> dict | None:
    """The stored answer for this key, or None when there is none (or it expired)."""
    item = dynamodb.get_item(TableName=table(), ConsistentRead=True,
                             Key={"pk": {"S": f"IDEMPOTENCY#{key}"}, "sk": {"S": "KEY"}}).get("Item")
    if item is None or int(item["expires_at"]["N"]) <= now():
        return None
    if item["fingerprint"]["S"] != fingerprint:
        raise Problem(422, "idempotency key reused")
    response = respond(int(item["status"]["N"]), json.loads(item["result"]["S"]))
    response["headers"]["idempotent-replayed"] = "true"
    return response


ROUTES = {
    "POST /events": create_event,
    "GET /events/{id}": get_event,
    "POST /events/{id}/signups": sign_up,
    "DELETE /events/{id}/signups/{email}": cancel,
    "GET /events/{id}/signups": list_signups,
}


def handler(event, context):
    route = ROUTES.get(event.get("routeKey", ""))
    if route is None:
        return respond(404, {"error": "no such route"})
    try:
        return route(event)
    except Problem as problem:
        return respond(problem.status, {"error": problem.message})
