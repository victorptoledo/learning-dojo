import pytest

from dojo_aws import iam, ref, resources, template
from dojo_aws.stack import ACCOUNT, Stack

TABLE = f"arn:aws:dynamodb:eu-west-1:{ACCOUNT}:table/dojo-SignupsTable"
ROLE = f"arn:aws:sts::{ACCOUNT}:assumed-role/dojo-SignupsFunctionRole/dojo-SignupsFunction"
ROUTES = {"POST /events", "GET /events/{id}", "POST /events/{id}/signups",
          "DELETE /events/{id}/signups/{email}", "GET /events/{id}/signups"}
USED = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:UpdateItem", "dynamodb:DeleteItem", "dynamodb:Query"]


@pytest.fixture(scope="module")
def tpl():
    return template("template.yaml")


@pytest.fixture(scope="module")
def stack(tpl):
    return Stack(tpl)


@pytest.fixture(scope="module")
def function(tpl):
    found = resources(tpl, "AWS::Serverless::Function")
    assert list(found) == ["SignupsFunction"], f"declare one function, SignupsFunction, found {sorted(found)}"
    merged = dict((tpl.get("Globals") or {}).get("Function") or {})
    merged.update(found["SignupsFunction"].get("Properties") or {})
    return merged


def route_keys(function):
    keys = {}
    for name, source in (function.get("Events") or {}).items():
        assert source.get("Type") == "HttpApi", f"event {name}: use HttpApi events"
        spec = source.get("Properties") or {}
        assert ref(spec.get("ApiId")) == "SignupsApi", f"event {name}: ApiId must be !Ref SignupsApi"
        keys[f"{spec.get('Method', '').upper()} {spec.get('Path')}"] = name
    return keys


def test_it_is_a_sam_template(tpl):
    assert tpl.get("Transform") == "AWS::Serverless-2016-10-31", "add Transform: AWS::Serverless-2016-10-31"


def test_the_api_is_throttled(tpl):
    found = resources(tpl, "AWS::Serverless::HttpApi")
    assert "SignupsApi" in found, "declare SignupsApi (AWS::Serverless::HttpApi)"
    settings = (found["SignupsApi"].get("Properties") or {}).get("DefaultRouteSettings") or {}
    burst, rate = settings.get("ThrottlingBurstLimit"), settings.get("ThrottlingRateLimit")
    assert burst is not None and 0 < int(burst) <= 100, f"ThrottlingBurstLimit must be set, at most 100, found {burst}"
    assert rate is not None and 0 < float(rate) <= 50, f"ThrottlingRateLimit must be set, at most 50, found {rate}"


def test_the_function_runs_the_handler_on_graviton(function, stack):
    assert function.get("CodeUri") in ("src/", "src", "./src"), "CodeUri must be src/"
    assert function.get("Handler") == "signups.handler", "Handler must be signups.handler"
    assert function.get("Runtime") == "python3.13", "Runtime must be python3.13"
    assert function.get("Architectures") == ["arm64"], "Architectures must be [arm64]"
    timeout = function.get("Timeout", 3)
    assert int(timeout) <= 10, f"Timeout must be at most 10 seconds, found {timeout}"
    variables = (function.get("Environment") or {}).get("Variables") or {}
    assert stack.value(variables.get("TABLE_NAME")) == "dojo-SignupsTable", "TABLE_NAME must be !Ref SignupsTable"


def test_the_routes_are_exactly_the_handlers(function):
    found = set(route_keys(function))
    assert found == ROUTES, f"the API must expose exactly {sorted(ROUTES)}, found {sorted(found)}"


def test_the_handler_answers_every_route_of_the_template(api, function):
    for route in route_keys(function):
        method, path = route.split(" ", 1)
        event = {"version": "2.0", "routeKey": route, "rawPath": path,
                 "pathParameters": {"id": "nope", "email": "ana@example.com"},
                 "headers": {"content-type": "application/json"},
                 "body": '{"email": "ana@example.com"}', "isBase64Encoded": False}
        answer = api.module.handler(event, None)
        assert "no such route" not in (answer.get("body") or ""), f"the handler does not answer {route}"


def test_logs_are_kept_for_30_days(tpl, stack):
    groups = resources(tpl, "AWS::Logs::LogGroup")
    assert "SignupsLogGroup" in groups, "declare SignupsLogGroup (AWS::Logs::LogGroup)"
    spec = groups["SignupsLogGroup"].get("Properties") or {}
    assert stack.value(spec.get("LogGroupName")) == "/aws/lambda/dojo-SignupsFunction", (
        'LogGroupName must be !Sub "/aws/lambda/${SignupsFunction}"')
    assert spec.get("RetentionInDays") == 30, "RetentionInDays must be 30"


def test_the_table_has_ttl_backups_and_encryption_and_survives_the_stack(tpl):
    tables = resources(tpl, "AWS::DynamoDB::Table")
    assert "SignupsTable" in tables, "declare SignupsTable (AWS::DynamoDB::Table)"
    table = tables["SignupsTable"]
    spec = table.get("Properties") or {}
    assert spec.get("BillingMode") == "PAY_PER_REQUEST", "BillingMode must be PAY_PER_REQUEST"
    keys = {k["AttributeName"]: k["KeyType"] for k in spec.get("KeySchema", [])}
    assert keys == {"pk": "HASH", "sk": "RANGE"}, f"the keys must be pk (HASH) and sk (RANGE), found {keys}"
    types = {a["AttributeName"]: a["AttributeType"] for a in spec.get("AttributeDefinitions", [])}
    assert types == {"pk": "S", "sk": "S"}, f"pk and sk are strings, found {types}"
    assert spec.get("TimeToLiveSpecification") == {"AttributeName": "expires_at", "Enabled": True}, (
        "turn TTL on for expires_at")
    assert (spec.get("PointInTimeRecoverySpecification") or {}).get("PointInTimeRecoveryEnabled") is True, (
        "turn point-in-time recovery on")
    assert (spec.get("SSESpecification") or {}).get("SSEEnabled") is True, "set SSESpecification: { SSEEnabled: true }"
    assert table.get("DeletionPolicy") == "Retain" and table.get("UpdateReplacePolicy") == "Retain", (
        "the table must survive the stack: DeletionPolicy and UpdateReplacePolicy Retain")


@pytest.fixture(scope="module")
def policy(function, stack):
    items = function.get("Policies")
    assert isinstance(items, list) and items, "give the function Policies: a list with one inline statement"
    for item in items:
        assert isinstance(item, dict) and "Statement" in item and len(item) == 1, (
            f"use only inline statements, no SAM policy templates or managed policies: found {item}")
    statements = [s for item in items for s in iam.as_list(stack.value(item["Statement"]))]
    document = {"Version": "2012-10-17", "Statement": statements}
    assert iam.validate(document) == [], "; ".join(iam.validate(document))
    return document


@pytest.mark.parametrize("action", USED)
def test_the_function_may_do_what_the_handler_does(policy, action):
    request = iam.Request(principal=ROLE, action=action, resource=TABLE)
    assert iam.evaluate(request, identity=[policy]) == iam.ALLOW, f"the function needs {action} on the table"


@pytest.mark.parametrize("case,action,resource", [
    ("scan the table", "dynamodb:Scan", TABLE),
    ("batch writes", "dynamodb:BatchWriteItem", TABLE),
    ("delete the table", "dynamodb:DeleteTable", TABLE),
    ("change the table", "dynamodb:UpdateTable", TABLE),
    ("turn TTL off", "dynamodb:UpdateTimeToLive", TABLE),
    ("read another table", "dynamodb:GetItem", f"arn:aws:dynamodb:eu-west-1:{ACCOUNT}:table/Payments"),
    ("read a table in another region", "dynamodb:GetItem",
     f"arn:aws:dynamodb:us-east-1:{ACCOUNT}:table/dojo-SignupsTable"),
    ("export to S3", "dynamodb:ExportTableToPointInTime", TABLE),
    ("anything outside DynamoDB", "s3:GetObject", "arn:aws:s3:::payroll/2026.csv"),
])
def test_the_function_may_do_nothing_more(policy, case, action, resource):
    request = iam.Request(principal=ROLE, action=action, resource=resource)
    found = iam.evaluate(request, identity=[policy])
    assert found != iam.ALLOW, f"{case}: {action} on {resource} must not be allowed"


def test_the_resource_is_the_table_arn_not_a_wildcard(policy):
    for statement in policy["Statement"]:
        for resource in iam.as_list(statement.get("Resource")):
            assert resource == TABLE, f"name the table: Resource must be !GetAtt SignupsTable.Arn, found {resource}"
