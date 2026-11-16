import pytest

from dojo_aws import getatt, iam, ref, resources, template

ACCOUNT = "111122223333"
TAGGABLE = ["WorkerRole", "OrdersQueue", "OrdersDeadLetterQueue", "WorkerFunction", "WorkerLogGroup", "OnCallTopic"]


@pytest.fixture(scope="module")
def tpl():
    return template("stack.yaml")


def props(tpl, name):
    found = resources(tpl)
    assert name in found, f"declare the resource {name}"
    return found[name].get("Properties", {})


def alarm(tpl, name):
    found = resources(tpl, "AWS::CloudWatch::Alarm")
    assert name in found, f"declare the alarm {name}"
    return found[name]["Properties"]


def notifies_on_call(found):
    return [ref(action) for action in found.get("AlarmActions", [])] == ["OnCallTopic"]


@pytest.mark.parametrize("name", TAGGABLE)
def test_every_resource_carries_the_cost_tags(tpl, name):
    tags = {t["Key"]: t["Value"] for t in props(tpl, name).get("Tags", [])}
    assert tags.get("service") == "orders" and tags.get("cost-center") == "cc-1042", (
        f"{name} needs the tags service=orders and cost-center=cc-1042, found {tags}")


def test_error_log_lines_become_a_metric(tpl):
    filters = resources(tpl, "AWS::Logs::MetricFilter")
    assert len(filters) == 1, "add one AWS::Logs::MetricFilter"
    found = next(iter(filters.values()))["Properties"]
    assert ref(found.get("LogGroupName")) == "WorkerLogGroup"
    assert found.get("FilterPattern", "").replace(" ", "") == '{$.level="ERROR"}', (
        'use the JSON filter pattern { $.level = "ERROR" }')
    transformation = found.get("MetricTransformations", [{}])[0]
    assert (transformation.get("MetricNamespace"), transformation.get("MetricName")) == ("Acme/Orders", "ErrorLogs")
    assert str(transformation.get("MetricValue")) == "1"


def test_the_on_call_topic_is_encrypted_and_mails_on_call(tpl):
    topic = props(tpl, "OnCallTopic")
    assert topic.get("KmsMasterKeyId") == "alias/aws/sns"
    assert topic.get("Subscription") == [{"Protocol": "email", "Endpoint": {"Ref": "OnCallEmail"}}]


def topic_policy(tpl):
    policies = resources(tpl, "AWS::SNS::TopicPolicy")
    assert len(policies) == 1, "add one AWS::SNS::TopicPolicy on OnCallTopic"
    found = next(iter(policies.values()))["Properties"]
    assert [ref(t) for t in found.get("Topics", [])] == ["OnCallTopic"]
    # Resolve the template's references so the dojo's IAM evaluator can read the policy.
    document = found["PolicyDocument"]
    for statement in iam.statements(document):
        if ref(statement.get("Resource")) == "OnCallTopic":
            statement["Resource"] = f"arn:aws:sns:eu-west-1:{ACCOUNT}:on-call"
        for keys in (statement.get("Condition") or {}).values():
            for key, value in list(keys.items()):
                if ref(value) == "AWS::AccountId":
                    keys[key] = ACCOUNT
    return document


TOPIC = f"arn:aws:sns:eu-west-1:{ACCOUNT}:on-call"


def test_budgets_may_publish_for_this_account(tpl):
    request = iam.Request("budgets.amazonaws.com", "sns:Publish", TOPIC, context={"aws:SourceAccount": ACCOUNT})
    assert iam.evaluate(request, resource_policy=topic_policy(tpl)) == iam.ALLOW


@pytest.mark.parametrize("principal,source", [
    ("budgets.amazonaws.com", "999988887777"), ("budgets.amazonaws.com", None),
    ("events.amazonaws.com", ACCOUNT), ("arn:aws:iam::999988887777:root", ACCOUNT),
], ids=["budgets-for-another-account", "budgets-without-source", "another-service", "another-account"])
def test_nobody_else_may_publish(tpl, principal, source):
    context = {"aws:SourceAccount": source} if source else {}
    admin = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "*", "Resource": "*"}]}
    request = iam.Request(principal, "sns:Publish", TOPIC, context=context)
    assert iam.evaluate(request, identity=[admin], resource_policy=topic_policy(tpl)) != iam.ALLOW


def test_error_rate_alarm_uses_metric_math(tpl):
    found = alarm(tpl, "ErrorRateAlarm")
    metrics = {m["Id"]: m for m in found.get("Metrics", [])}
    returned = [m for m in metrics.values() if m.get("ReturnData", True)]
    assert len(returned) == 1 and "Expression" in returned[0], "alarm on one expression over Errors and Invocations"
    names = {m["MetricStat"]["Metric"]["MetricName"] for m in metrics.values() if "MetricStat" in m}
    assert names == {"Errors", "Invocations"}
    for m in metrics.values():
        if "MetricStat" in m:
            dims = m["MetricStat"]["Metric"].get("Dimensions", [])
            assert dims == [{"Name": "FunctionName", "Value": {"Ref": "WorkerFunction"}}]
            assert m["MetricStat"].get("Stat") == "Sum"
    assert (found.get("ComparisonOperator"), found.get("Threshold")) == ("GreaterThanThreshold", 1)
    assert (found.get("EvaluationPeriods"), found.get("DatapointsToAlarm")) == (5, 3), "3 of 5 datapoints"
    assert found.get("TreatMissingData") == "notBreaching"
    assert notifies_on_call(found) and [ref(a) for a in found.get("OKActions", [])] == ["OnCallTopic"]


def test_slow_worker_alarm_watches_p99_duration(tpl):
    found = alarm(tpl, "SlowWorkerAlarm")
    assert (found.get("Namespace"), found.get("MetricName")) == ("AWS/Lambda", "Duration")
    assert found.get("ExtendedStatistic") == "p99", "use ExtendedStatistic p99, not an average"
    assert found.get("Threshold") == 5000 and found.get("ComparisonOperator") == "GreaterThanThreshold"
    assert notifies_on_call(found)


@pytest.mark.parametrize("name,metric,queue,threshold", [
    ("QueueBacklogAlarm", "ApproximateAgeOfOldestMessage", "OrdersQueue", 300),
    ("DeadLettersAlarm", "ApproximateNumberOfMessagesVisible", "OrdersDeadLetterQueue", 0),
])
def test_queue_alarms(tpl, name, metric, queue, threshold):
    found = alarm(tpl, name)
    assert (found.get("Namespace"), found.get("MetricName")) == ("AWS/SQS", metric)
    dims = found.get("Dimensions", [])
    assert len(dims) == 1 and dims[0]["Name"] == "QueueName" and getatt(dims[0]["Value"]) == (queue, "QueueName")
    assert found.get("Statistic") == "Maximum"
    assert found.get("Threshold") == threshold and found.get("ComparisonOperator") == "GreaterThanThreshold"
    assert notifies_on_call(found)


def test_error_logs_alarm_reads_the_custom_metric(tpl):
    found = alarm(tpl, "ErrorLogsAlarm")
    assert (found.get("Namespace"), found.get("MetricName"), found.get("Statistic")) == ("Acme/Orders", "ErrorLogs", "Sum")
    assert notifies_on_call(found)


def test_every_alarm_treats_missing_data_deliberately(tpl):
    for name, found in resources(tpl, "AWS::CloudWatch::Alarm").items():
        assert found["Properties"].get("TreatMissingData") == "notBreaching", f"{name}: set TreatMissingData"


def test_the_budget_alerts_before_the_month_is_over(tpl):
    budgets = resources(tpl, "AWS::Budgets::Budget")
    assert len(budgets) == 1, "add one AWS::Budgets::Budget"
    found = next(iter(budgets.values()))["Properties"]
    budget = found["Budget"]
    assert (budget.get("BudgetType"), budget.get("TimeUnit")) == ("COST", "MONTHLY")
    assert budget.get("BudgetLimit") == {"Amount": {"Ref": "MonthlyBudgetUsd"}, "Unit": "USD"}
    assert budget.get("CostFilters", {}).get("TagKeyValue") == ["user:service$orders"], (
        'filter on the cost allocation tag: TagKeyValue ["user:service$orders"]')
    notes = {(n["Notification"]["NotificationType"], n["Notification"]["Threshold"]):
             [ref(s.get("Address")) for s in n.get("Subscribers", [])]
             for n in found.get("NotificationsWithSubscribers", [])}
    assert notes == {("ACTUAL", 80): ["OnCallTopic"], ("FORECASTED", 100): ["OnCallTopic"]}, f"found {notes}"
