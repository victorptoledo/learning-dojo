import pytest

from dojo_aws import getatt, outputs, ref, resources, template

ALLOWED_POLICY_TEMPLATES = {"DynamoDBCrudPolicy", "DynamoDBReadPolicy", "SQSSendMessagePolicy",
                            "SQSPollerPolicy"}


@pytest.fixture(scope="module")
def tpl():
    return template("template.yaml")


def props(res):
    return res.get("Properties", {})


def function(tpl, name):
    found = resources(tpl, "AWS::Serverless::Function")
    assert name in found, f"declare the function {name}"
    return props(found[name])


def test_is_a_sam_template(tpl):
    assert tpl.get("Transform") == "AWS::Serverless-2016-10-31"


@pytest.mark.parametrize("setting,value", [
    ("Architectures", ["arm64"]),
    ("MemorySize", 256),
    ("Timeout", 10),
    ("Tracing", "Active"),
])
def test_global_function_settings(tpl, setting, value):
    found = tpl.get("Globals", {}).get("Function", {})
    assert found.get(setting) == value, f"Globals.Function.{setting} must be {value}"


def test_the_table_is_protected(tpl):
    table = resources(tpl)["OrdersTable"]
    assert table.get("DeletionPolicy") == "Retain" and table.get("UpdateReplacePolicy") == "Retain", (
        "set DeletionPolicy and UpdateReplacePolicy to Retain on OrdersTable")
    assert props(table).get("PointInTimeRecoverySpecification", {}).get("PointInTimeRecoveryEnabled") is True
    assert props(table).get("SSESpecification", {}).get("SSEEnabled") is True


def queue(tpl, name):
    found = resources(tpl, "AWS::SQS::Queue")
    assert name in found, f"declare the queue {name}"
    return props(found[name])


def test_the_dead_letter_queue_keeps_messages_for_14_days(tpl):
    dlq = queue(tpl, "OrderEventsDeadLetterQueue")
    assert dlq.get("MessageRetentionPeriod") == 1209600
    assert dlq.get("SqsManagedSseEnabled") is True


def test_the_queue_redrives_to_the_dead_letter_queue(tpl):
    main = queue(tpl, "OrderEventsQueue")
    redrive = main.get("RedrivePolicy", {})
    assert getatt(redrive.get("deadLetterTargetArn")) == ("OrderEventsDeadLetterQueue", "Arn")
    assert redrive.get("maxReceiveCount") == 5
    assert main.get("SqsManagedSseEnabled") is True


def test_the_visibility_timeout_covers_six_function_timeouts(tpl):
    timeout = tpl.get("Globals", {}).get("Function", {}).get("Timeout", 3)
    visibility = queue(tpl, "OrderEventsQueue").get("VisibilityTimeout", 30)
    assert visibility >= 6 * timeout, (
        f"VisibilityTimeout {visibility} s must be at least 6 x the function timeout ({timeout} s), "
        "or messages come back while they are still being processed")


def test_the_api_serves_post_and_get(tpl):
    events = function(tpl, "ApiFunction").get("Events", {}).values()
    routes = {(e.get("Properties", {}).get("Method", "").upper(), e.get("Properties", {}).get("Path"))
              for e in events if e.get("Type") == "HttpApi"}
    assert routes == {("POST", "/orders"), ("GET", "/orders/{id}")}, f"found {routes}"


def test_the_api_knows_the_queue_url(tpl):
    variables = function(tpl, "ApiFunction").get("Environment", {}).get("Variables", {})
    assert ref(variables.get("QUEUE_URL")) == "OrderEventsQueue", "QUEUE_URL must be !Ref OrderEventsQueue"


def test_the_worker_reads_the_queue_in_batches_with_partial_failures(tpl):
    events = [e for e in function(tpl, "WorkerFunction").get("Events", {}).values() if e.get("Type") == "SQS"]
    assert len(events) == 1, "WorkerFunction needs one SQS event"
    found = events[0].get("Properties", {})
    assert getatt(found.get("Queue")) == ("OrderEventsQueue", "Arn")
    assert found.get("BatchSize") == 10
    assert found.get("FunctionResponseTypes") == ["ReportBatchItemFailures"]


def policy_templates(policies):
    names = []
    for policy in policies:
        assert isinstance(policy, dict) and len(policy) == 1, (
            f"use SAM policy templates such as DynamoDBCrudPolicy, not {policy}")
        names.append(next(iter(policy)))
    return names


@pytest.mark.parametrize("name,expected", [
    ("ApiFunction", {"DynamoDBCrudPolicy": "OrdersTable", "SQSSendMessagePolicy": "OrderEventsQueue"}),
    ("WorkerFunction", {"DynamoDBCrudPolicy": "OrdersTable"}),
])
def test_functions_get_least_privilege_policies(tpl, name, expected):
    policies = function(tpl, name).get("Policies", [])
    names = policy_templates(policies)
    assert set(names) <= ALLOWED_POLICY_TEMPLATES, f"{name}: no managed FullAccess policies, found {names}"
    assert sorted(names) == sorted(expected), f"{name} needs exactly {sorted(expected)}, found {names}"
    for policy in policies:
        (kind, arguments), = policy.items()
        target = arguments.get("TableName") or arguments.get("QueueName")
        assert ref(target) == expected[kind] or getatt(target) == (expected[kind], "QueueName"), (
            f"{name}: {kind} must point at {expected[kind]}")


@pytest.mark.parametrize("function_name", ["ApiFunction", "WorkerFunction"])
def test_each_function_has_a_log_group_with_retention(tpl, function_name):
    groups = [props(g) for g in resources(tpl, "AWS::Logs::LogGroup").values()
              if props(g).get("LogGroupName") == {"Fn::Sub": f"/aws/lambda/${{{function_name}}}"}]
    assert groups, f'add a log group named !Sub "/aws/lambda/${{{function_name}}}"'
    assert groups[0].get("RetentionInDays") == 30


def test_outputs_the_api_url_and_dead_letter_queue(tpl):
    found = outputs(tpl)
    assert "ApiUrl" in found
    assert ref(found.get("DeadLetterQueueUrl", {}).get("Value")) == "OrderEventsDeadLetterQueue"
