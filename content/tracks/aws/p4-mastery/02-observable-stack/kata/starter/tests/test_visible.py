from dojo_aws import resources, template


def test_the_log_group_keeps_logs_for_30_days():
    group = resources(template("stack.yaml"), "AWS::Logs::LogGroup")["WorkerLogGroup"]
    assert group["Properties"].get("RetentionInDays") == 30
