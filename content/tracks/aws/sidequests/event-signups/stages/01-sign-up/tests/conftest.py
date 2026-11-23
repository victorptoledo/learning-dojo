"""The handler under test, called with HTTP API (payload 2.0) events inside moto.

Every test gets a fresh in-memory Signups table, a freshly imported `signups` module (so its
module-level client is created inside moto) and a log of the DynamoDB requests it sends.
"""

import importlib
import json
import re
import sys
from dataclasses import dataclass, field

import boto3
import pytest
from moto import mock_aws

TABLE = "Signups"
ROUTES = [
    ("POST", "/events"),
    ("GET", "/events/{id}"),
    ("POST", "/events/{id}/signups"),
    ("DELETE", "/events/{id}/signups/{email}"),
    ("GET", "/events/{id}/signups"),
]


def match_route(method: str, path: str) -> tuple[str, dict | None]:
    for route_method, template in ROUTES:
        pattern = "^" + re.sub(r"\{(\w+)\}", r"(?P<\1>[^/]+)", template) + "$"
        found = re.match(pattern, path)
        if route_method == method and found:
            return f"{method} {template}", found.groupdict() or None
    return "$default", None


@dataclass
class Response:
    status: int
    body: dict | None
    headers: dict


@dataclass
class Api:
    module: object
    table: object  # a DynamoDB client of the test's own, not recorded
    calls: list = field(default_factory=list)

    def call(self, method, path, body=None, headers=None, query=None, raw=None) -> Response:
        route_key, parameters = match_route(method, path)
        event = {
            "version": "2.0",
            "routeKey": route_key,
            "rawPath": path,
            "rawQueryString": "&".join(f"{k}={v}" for k, v in (query or {}).items()),
            "headers": {"content-type": "application/json", **{k.lower(): v for k, v in (headers or {}).items()}},
            "requestContext": {"http": {"method": method, "path": path}, "stage": "$default"},
            "isBase64Encoded": False,
        }
        if parameters:
            event["pathParameters"] = parameters
        if query:
            event["queryStringParameters"] = query
        if raw is not None:
            event["body"] = raw
        elif body is not None:
            event["body"] = json.dumps(body)
        answer = self.module.handler(event, None)
        assert isinstance(answer, dict) and isinstance(answer.get("statusCode"), int), (
            f"{method} {path}: the handler must return a dict with an int statusCode, returned {answer!r}")
        text = answer.get("body")
        parsed = json.loads(text) if text else None
        return Response(answer["statusCode"], parsed, answer.get("headers") or {})

    def operations(self) -> list[str]:
        return [name for name, _ in self.calls]

    def item(self, pk, sk):
        return self.table.get_item(TableName=TABLE, Key={"pk": {"S": pk}, "sk": {"S": sk}},
                                   ConsistentRead=True).get("Item")

    def items(self, prefix=""):
        found = self.table.scan(TableName=TABLE, ConsistentRead=True)["Items"]
        return sorted((i["pk"]["S"], i["sk"]["S"]) for i in found if i["pk"]["S"].startswith(prefix))

    def event(self, event_id="rust-meetup", capacity=3, title="Rust meetup"):
        answer = self.call("POST", "/events", {"id": event_id, "title": title, "capacity": capacity})
        assert answer.status == 201, f"POST /events must answer 201, answered {answer.status} {answer.body}"
        return event_id

    def sign_up(self, email, event_id="rust-meetup", **options):
        return self.call("POST", f"/events/{event_id}/signups", {"email": email}, **options)


@pytest.fixture
def api(monkeypatch):
    monkeypatch.setenv("TABLE_NAME", TABLE)
    with mock_aws():
        boto3.setup_default_session(region_name="eu-west-1")
        calls = []
        boto3.DEFAULT_SESSION.events.register(
            "provide-client-params.dynamodb.*",
            lambda model, params, **_: calls.append((model.name, params)))
        table = boto3.Session(region_name="eu-west-1").client("dynamodb")
        table.create_table(
            TableName=TABLE,
            BillingMode="PAY_PER_REQUEST",
            KeySchema=[{"AttributeName": "pk", "KeyType": "HASH"},
                       {"AttributeName": "sk", "KeyType": "RANGE"}],
            AttributeDefinitions=[{"AttributeName": "pk", "AttributeType": "S"},
                                  {"AttributeName": "sk", "AttributeType": "S"}],
        )
        sys.modules.pop("signups", None)
        module = importlib.import_module("signups")
        yield Api(module, table, calls)
        sys.modules.pop("signups", None)
        boto3.DEFAULT_SESSION = None
