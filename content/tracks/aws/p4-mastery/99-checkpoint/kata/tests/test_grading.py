import pytest

from dojo_aws import getatt, iam, ref, resources, template
from dojo_aws.stack import ACCOUNT, REGION, Stack

TAGGED = ["InvoicesKey", "InvoicesBucket", "InvoicesTable", "InvoiceQueue", "InvoiceDeadLetterQueue",
          "WorkerRole", "WorkerFunction", "OnCallTopic"]
EVERYTHING = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "*", "Resource": "*"}]}


@pytest.fixture(scope="module")
def tpl():
    return template("service.yaml")


@pytest.fixture(scope="module")
def dev(tpl):
    return Stack(tpl, {"Environment": "dev"})


@pytest.fixture(scope="module")
def prod(tpl):
    return Stack(tpl, {"Environment": "prod"})


def props(stack, name):
    return stack.value(stack.props(name))


def raw(tpl, name):
    found = resources(tpl)
    assert name in found, f"declare {name}"
    return found[name]


def by_type(stack, type_):
    return {name: stack.value(res.get("Properties", {})) for name, res in stack.resources_of(type_).items()}


# ---- security ------------------------------------------------------------------------

def worker_policies(stack):
    found = props(stack, "WorkerRole").get("Policies", [])
    return [policy["PolicyDocument"] for policy in found]


def worker(stack, action, resource):
    request = iam.Request(principal=stack.attribute("WorkerRole", "Arn"), action=action, resource=resource)
    return iam.evaluate(request, identity=worker_policies(stack))


def needs(stack):
    bucket, table = stack.attribute("InvoicesBucket", "Arn"), stack.attribute("InvoicesTable", "Arn")
    queue, key = stack.attribute("InvoiceQueue", "Arn"), stack.attribute("InvoicesKey", "Arn")
    return [
        ("sqs:ReceiveMessage", queue), ("sqs:DeleteMessage", queue), ("sqs:GetQueueAttributes", queue),
        ("dynamodb:PutItem", table), ("dynamodb:GetItem", table),
        ("s3:PutObject", f"{bucket}/invoices/inv-1.json"),
        ("kms:GenerateDataKey", key),
    ]


def test_the_worker_can_still_do_its_job(dev):
    for action, resource in needs(dev):
        assert worker(dev, action, resource) == iam.ALLOW, f"the worker needs {action} on {resource}"


@pytest.mark.parametrize("action,target", [
    ("dynamodb:DeleteTable", "table"), ("dynamodb:Scan", "table"), ("dynamodb:PutItem", "other-table"),
    ("s3:DeleteObject", "objects"), ("s3:GetObject", "objects"), ("s3:PutObject", "other-bucket"),
    ("s3:PutObject", "other-prefix"),
    ("s3:PutBucketPolicy", "bucket"), ("sqs:PurgeQueue", "queue"), ("sqs:SendMessage", "queue"),
    ("sqs:ReceiveMessage", "dead-letters"), ("kms:Decrypt", "other-key"), ("iam:PassRole", "*"),
])
def test_the_worker_can_do_nothing_else(dev, action, target):
    targets = {
        "table": dev.attribute("InvoicesTable", "Arn"),
        "other-table": f"arn:aws:dynamodb:{REGION}:{ACCOUNT}:table/payroll",
        "objects": dev.attribute("InvoicesBucket", "Arn") + "/invoices/inv-1.json",
        "bucket": dev.attribute("InvoicesBucket", "Arn"),
        "other-bucket": "arn:aws:s3:::payroll-exports/x.json",
        "other-prefix": dev.attribute("InvoicesBucket", "Arn") + "/exports/all.csv",
        "queue": dev.attribute("InvoiceQueue", "Arn"),
        "dead-letters": dev.attribute("InvoiceDeadLetterQueue", "Arn") if "InvoiceDeadLetterQueue" in dev.resources
        else f"arn:aws:sqs:{REGION}:{ACCOUNT}:dlq",
        "other-key": f"arn:aws:kms:{REGION}:{ACCOUNT}:key/0000aaaa-0000-4000-8000-000000000000",
        "*": "*",
    }
    assert worker(dev, action, targets[target]) != iam.ALLOW, f"the worker may still {action} on {targets[target]}"


def test_pdfs_are_encrypted_with_our_own_key(tpl, dev):
    key = raw(tpl, "InvoicesKey")
    assert key.get("Type") == "AWS::KMS::Key"
    assert key.get("DeletionPolicy") == "Retain", "deleting the stack must not make every PDF unreadable"
    assert key["Properties"].get("EnableKeyRotation") is True
    rules = raw(tpl, "InvoicesBucket").get("Properties", {}).get("BucketEncryption", {}).get(
        "ServerSideEncryptionConfiguration", [])
    assert rules, "encrypt the bucket with InvoicesKey"
    default = rules[0].get("ServerSideEncryptionByDefault", {})
    assert default.get("SSEAlgorithm") == "aws:kms" and getatt(default.get("KMSMasterKeyID")) == ("InvoicesKey", "Arn")


def test_the_bucket_refuses_plain_http(dev):
    policies = by_type(dev, "AWS::S3::BucketPolicy")
    assert policies, "add an AWS::S3::BucketPolicy on InvoicesBucket"
    (policy,) = policies.values()
    assert policy.get("Bucket") == dev.ref("InvoicesBucket")
    document = policy["PolicyDocument"]
    role = dev.attribute("WorkerRole", "Arn")
    bucket = dev.attribute("InvoicesBucket", "Arn")
    for action, resource in (("s3:PutObject", f"{bucket}/invoices/inv-1.json"), ("s3:ListBucket", bucket)):
        insecure = iam.Request(principal=role, action=action, resource=resource,
                               context={"aws:SecureTransport": "false"})
        assert iam.evaluate(insecure, identity=[EVERYTHING], resource_policy=document) == iam.EXPLICIT_DENY, (
            f"deny {action} on {resource} when aws:SecureTransport is false")
        secure = iam.Request(principal=role, action=action, resource=resource, context={"aws:SecureTransport": "true"})
        assert iam.evaluate(secure, identity=[EVERYTHING], resource_policy=document) == iam.ALLOW


# ---- reliability ---------------------------------------------------------------------

def test_poison_messages_go_to_a_dead_letter_queue(tpl, dev):
    queue = props(dev, "InvoiceQueue")
    redrive = raw(tpl, "InvoiceQueue")["Properties"].get("RedrivePolicy", {})
    assert getatt(redrive.get("deadLetterTargetArn")) == ("InvoiceDeadLetterQueue", "Arn"), (
        "redrive InvoiceQueue to InvoiceDeadLetterQueue")
    assert redrive.get("maxReceiveCount") == 5
    assert props(dev, "InvoiceDeadLetterQueue").get("MessageRetentionPeriod") == 1209600
    timeout = props(dev, "WorkerFunction").get("Timeout", 3)
    assert queue.get("VisibilityTimeout", 30) >= 6 * timeout, "visibility timeout: six times the function timeout"


def test_one_bad_invoice_does_not_retry_the_batch(dev):
    mapping = props(dev, "WorkerEventSource")
    assert "ReportBatchItemFailures" in mapping.get("FunctionResponseTypes", []), (
        "the handler returns batchItemFailures, but the mapping ignores them without ReportBatchItemFailures")


def test_data_outlives_the_stack(tpl):
    for name in ("InvoicesBucket", "InvoicesTable"):
        found = raw(tpl, name)
        assert found.get("DeletionPolicy") == "Retain" and found.get("UpdateReplacePolicy") == "Retain", (
            f"set DeletionPolicy and UpdateReplacePolicy to Retain on {name}")


def test_overwritten_pdfs_can_be_recovered(dev):
    versioning = props(dev, "InvoicesBucket").get("VersioningConfiguration", {})
    assert versioning.get("Status") == "Enabled"


def test_the_table_is_protected_in_prod_only(dev, prod):
    assert props(prod, "InvoicesTable").get("DeletionProtectionEnabled") is True, "protect the prod table"
    assert props(dev, "InvoicesTable").get("DeletionProtectionEnabled") in (False, None), (
        "dev tables are thrown away: use !If [IsProd, true, false]")
    assert props(prod, "InvoicesTable").get("PointInTimeRecoverySpecification", {}).get(
        "PointInTimeRecoveryEnabled") is True


# ---- operational excellence ----------------------------------------------------------

def test_only_prod_pages_the_on_call(dev, prod):
    assert props(prod, "OnCallTopic").get("KmsMasterKeyId") == "alias/aws/sns"
    pages = by_type(prod, "AWS::SNS::Subscription")
    assert [(s.get("Protocol"), s.get("Endpoint"), s.get("TopicArn")) for s in pages.values()] == [
        ("email", "invoices-oncall@example.com", prod.ref("OnCallTopic"))], "one email subscription to OnCallEmail"
    assert not by_type(dev, "AWS::SNS::Subscription"), "dev must not page anyone: Condition: IsProd"


def alarm_on(stack, namespace, metric):
    found = [a for a in by_type(stack, "AWS::CloudWatch::Alarm").values()
             if a.get("Namespace") == namespace and a.get("MetricName") == metric]
    assert len(found) == 1, f"add one alarm on {namespace} {metric}"
    alarm = found[0]
    assert alarm.get("AlarmActions") == [stack.ref("OnCallTopic")], "alarms notify OnCallTopic"
    assert alarm.get("TreatMissingData") == "notBreaching"
    assert alarm.get("ComparisonOperator") == "GreaterThanThreshold"
    return alarm


def test_dead_letters_raise_an_alarm(dev):
    alarm = alarm_on(dev, "AWS/SQS", "ApproximateNumberOfMessagesVisible")
    assert alarm.get("Dimensions") == [{"Name": "QueueName", "Value": "InvoiceDeadLetterQueue.QueueName"}], (
        "watch the dead-letter queue: QueueName is !GetAtt InvoiceDeadLetterQueue.QueueName")
    assert alarm.get("Statistic") == "Maximum" and alarm.get("Threshold") == 0


def test_a_stuck_queue_raises_an_alarm(dev):
    alarm = alarm_on(dev, "AWS/SQS", "ApproximateAgeOfOldestMessage")
    assert alarm.get("Dimensions") == [{"Name": "QueueName", "Value": "InvoiceQueue.QueueName"}]
    assert alarm.get("Statistic") == "Maximum" and alarm.get("Threshold") == 600


def test_logs_are_kept_by_environment(dev, prod):
    for stack, days in ((dev, 14), (prod, 90)):
        groups = by_type(stack, "AWS::Logs::LogGroup")
        assert len(groups) == 1, "declare the worker's log group"
        (group,) = groups.values()
        assert group.get("LogGroupName") == f"/aws/lambda/{stack.ref('WorkerFunction')}"
        assert group.get("RetentionInDays") == days, f"keep {days} days of logs in {stack.parameters['Environment']}"


# ---- performance efficiency ----------------------------------------------------------

def test_the_worker_reads_in_batches(dev):
    mapping = props(dev, "WorkerEventSource")
    assert mapping.get("BatchSize") == 10, "one message per invocation pays the invocation overhead each time"
    assert mapping.get("MaximumBatchingWindowInSeconds") == 5


# ---- cost optimization ---------------------------------------------------------------

def test_spiky_traffic_pays_per_request(dev):
    table = props(dev, "InvoicesTable")
    assert table.get("BillingMode") == "PAY_PER_REQUEST" and "ProvisionedThroughput" not in table, (
        "500 provisioned units sit idle most of the day: use on-demand")


def test_old_pdfs_move_to_infrequent_access_with_a_bucket_key(dev):
    bucket = props(dev, "InvoicesBucket")
    transitions = [t for rule in bucket.get("LifecycleConfiguration", {}).get("Rules", [])
                   if rule.get("Status") == "Enabled" for t in rule.get("Transitions", [])]
    assert {"StorageClass": "STANDARD_IA", "TransitionInDays": 30} in transitions
    rules = bucket.get("BucketEncryption", {}).get("ServerSideEncryptionConfiguration", [])
    assert rules and rules[0].get("BucketKeyEnabled") is True, (
        "turn on the S3 Bucket Key: one data key per bucket instead of a KMS call per object")


@pytest.mark.parametrize("name", TAGGED)
def test_costs_are_tagged(prod, name):
    tags = {tag["Key"]: tag["Value"] for tag in props(prod, name).get("Tags", [])}
    wanted = {"service": "invoices", "cost-center": "cc-2077", "environment": "prod"}
    assert {key: tags.get(key) for key in wanted} == wanted, f"tag {name} (environment: !Ref Environment)"


# ---- sustainability ------------------------------------------------------------------

def test_the_worker_runs_on_graviton(dev):
    assert props(dev, "WorkerFunction").get("Architectures") == ["arm64"]


def test_nothing_is_kept_that_nobody_needs(dev):
    table = props(dev, "InvoicesTable")
    assert table.get("TimeToLiveSpecification") == {"AttributeName": "expires_at", "Enabled": True}
    rules = [rule for rule in props(dev, "InvoicesBucket").get("LifecycleConfiguration", {}).get("Rules", [])
             if rule.get("Status") == "Enabled"]
    assert any(rule.get("NoncurrentVersionExpiration", {}).get("NoncurrentDays") == 30 for rule in rules), (
        "expire old versions after 30 days")
    assert any(rule.get("AbortIncompleteMultipartUpload", {}).get("DaysAfterInitiation") == 7 for rule in rules)


# ---- the whole -----------------------------------------------------------------------

def test_the_service_keeps_its_shape(tpl):
    for name in ("InvoicesBucket", "InvoicesTable", "InvoiceQueue", "WorkerRole", "WorkerFunction",
                 "WorkerEventSource"):
        raw(tpl, name)
    function = raw(tpl, "WorkerFunction")["Properties"]
    assert getatt(function.get("Role")) == ("WorkerRole", "Arn")
    assert ref(function["Environment"]["Variables"].get("TABLE_NAME")) == "InvoicesTable"
    assert ref(function["Environment"]["Variables"].get("BUCKET_NAME")) == "InvoicesBucket"
