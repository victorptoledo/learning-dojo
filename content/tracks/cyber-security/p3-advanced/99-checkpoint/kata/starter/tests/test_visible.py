from gate import detect, iam_findings, log_line, workflow_findings


def test_a_service_wildcard_is_flagged():
    policy = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": ["s3:*"], "Resource": "*"}]}
    assert iam_findings(policy) == ["0:action-wildcard"]


def test_a_tag_is_not_a_pin():
    flow = {"on": "push", "permissions": {"contents": "read"}, "jobs": {"build": {"steps": [{"uses": "actions/checkout@v4"}]}}}
    assert workflow_findings(flow) == ["unpinned:actions/checkout"]


def test_one_user_failing_many_times_is_not_spraying():
    events = [{"ts": i, "ip": "203.0.113.9", "user": "ada", "outcome": "failure"} for i in range(6)]
    assert detect(events) == []


def test_log_lines_are_json_and_redacted():
    assert log_line({"user": "ada", "password": "hunter2"}) == '{"password":"[REDACTED]","user":"ada"}'
