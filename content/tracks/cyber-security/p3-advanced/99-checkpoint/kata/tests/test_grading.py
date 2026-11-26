import json

import pytest

from gate import detect, iam_findings, log_line, workflow_findings

SHA = "8e5e7e5ab8b370d6c329ec480221332ada57f0ab"
DIGEST = "sha256:" + "a1" * 32


def policy(*statements):
    return {"Version": "2012-10-17", "Statement": list(statements)}


def allow(action, resource="arn:aws:s3:::reports/*", **extra):
    return {"Effect": "Allow", "Action": action, "Resource": resource, **extra}


# ---- Part 1: IAM ------------------------------------------------------------


def test_a_least_privilege_policy_is_clean():
    assert iam_findings(policy(allow(["s3:GetObject", "s3:PutObject"]))) == []


def test_admin_and_service_wildcards_in_string_or_list():
    findings = iam_findings(policy(allow("*", "*"), allow(["s3:GetObject", "s3:*"]), allow("ec2:Describe*")))
    assert findings == ["0:admin", "1:action-wildcard", "2:action-wildcard"]


def test_a_single_statement_object_is_accepted():
    assert iam_findings({"Version": "2012-10-17", "Statement": allow("*", "*")}) == ["0:admin"]


def test_deny_statements_are_not_findings():
    assert iam_findings(policy({"Effect": "Deny", "Action": "*", "Resource": "*"})) == []


def test_an_unknown_effect_fails_closed():
    with pytest.raises(ValueError):
        iam_findings(policy({"Effect": "allow", "Action": "s3:GetObject", "Resource": "*"}))
    with pytest.raises(ValueError):
        iam_findings(policy({"Action": "s3:GetObject", "Resource": "*"}))


def test_allow_with_not_action_grants_everything_else():
    statement = {"Effect": "Allow", "NotAction": "iam:*", "Resource": "*"}
    assert iam_findings(policy(statement)) == ["0:not-action"]


def test_pass_role_on_any_role_ignores_action_case():
    assert iam_findings(policy(allow("IAM:PassRole", "*"))) == ["0:passrole-any"]
    scoped = allow("iam:PassRole", "arn:aws:iam::123456789012:role/app-runtime")
    assert iam_findings(policy(scoped)) == []
    assert iam_findings(policy(allow(["iam:passrole", "s3:GetObject"], ["arn:x", "*"]))) == ["0:passrole-any"]


def test_public_principals_unless_a_condition_limits_them():
    star = allow("s3:GetObject", Principal="*")
    aws_star = allow("s3:GetObject", Principal={"AWS": ["arn:aws:iam::123456789012:root", "*"]})
    limited = allow("s3:GetObject", Principal="*", Condition={"StringEquals": {"aws:SourceVpce": "vpce-1a2b"}})
    account = allow("s3:GetObject", Principal={"AWS": "arn:aws:iam::123456789012:root"})
    assert iam_findings(policy(star, aws_star, limited, account)) == ["0:public", "1:public"]


def test_findings_keep_their_order_inside_a_statement():
    statement = allow(["s3:*", "iam:PassRole"], "*", Principal="*")
    assert iam_findings(policy(statement)) == ["0:action-wildcard", "0:passrole-any", "0:public"]


# ---- Part 2: the CI workflow ------------------------------------------------


def workflow(steps, on="push", **top):
    return {"on": on, "permissions": {"contents": "read"}, **top, "jobs": {"build": {"steps": steps}}}


def test_a_pinned_least_privilege_workflow_is_clean():
    steps = [
        {"uses": f"actions/checkout@{SHA}"},
        {"uses": "./.github/actions/setup"},
        {"uses": f"docker://alpine@{DIGEST}"},
        {"run": "npm ci && npm test"},
    ]
    assert workflow_findings(workflow(steps)) == []


def test_tags_branches_short_shas_and_missing_refs_are_unpinned():
    steps = [
        {"uses": "actions/checkout@v4"},
        {"uses": "actions/setup-node@main"},
        {"uses": "acme/deploy@8e5e7e5"},
        {"uses": "acme/scan"},
        {"uses": "docker://alpine:3.22"},
        {"uses": f"actions/cache@{SHA}"},
    ]
    assert workflow_findings(workflow(steps)) == [
        "unpinned:acme/deploy",
        "unpinned:acme/scan",
        "unpinned:actions/checkout",
        "unpinned:actions/setup-node",
        "unpinned:docker://alpine:3.22",
    ]


def test_token_permissions_missing_or_write_all():
    steps = [{"run": "make"}]
    missing = {"on": "push", "jobs": {"build": {"steps": steps}}}
    assert workflow_findings(missing) == ["token-permissions"]
    assert workflow_findings(workflow(steps, permissions="write-all")) == ["token-permissions"]
    job_level = workflow(steps)
    job_level["jobs"]["build"]["permissions"] = "write-all"
    assert workflow_findings(job_level) == ["token-permissions"]


def test_untrusted_input_in_a_run_script_is_script_injection():
    steps = [
        {"run": 'echo "Title: ${{ github.event.pull_request.title }}"'},
        {"run": "git switch ${{github.head_ref}}"},
    ]
    assert workflow_findings(workflow(steps)) == ["script-injection:build"]
    assert workflow_findings(workflow(steps[1:])) == ["script-injection:build"]
    safe = [{"run": "echo ${{ github.sha }} ${{ secrets.NPM_TOKEN != '' }}"}]
    assert workflow_findings(workflow(safe)) == []


def test_pull_request_target_checking_out_the_fork_code():
    steps = [{"uses": f"actions/checkout@{SHA}", "with": {"ref": "${{ github.event.pull_request.head.sha }}"}}]
    assert workflow_findings(workflow(steps, on={"pull_request_target": {"types": ["opened"]}})) == [
        "untrusted-checkout"
    ]
    assert workflow_findings(workflow(steps, on=["pull_request"])) == []
    base_only = [{"uses": f"actions/checkout@{SHA}"}]
    assert workflow_findings(workflow(base_only, on="pull_request_target")) == []


def test_the_on_key_read_by_yaml_as_true():
    steps = [{"uses": f"actions/checkout@{SHA}", "with": {"ref": "${{ github.head_ref }}"}}]
    parsed = {True: ["pull_request_target"], "permissions": {}, "jobs": {"build": {"steps": steps}}}
    assert workflow_findings(parsed) == ["untrusted-checkout"]


def test_long_lived_cloud_keys_name_the_job():
    flow = workflow([])
    flow["jobs"]["deploy"] = {
        "steps": [
            {
                "uses": f"aws-actions/configure-aws-credentials@{SHA}",
                "with": {"AWS-Access-Key-Id": "${{ secrets.KEY }}", "aws-region": "sa-east-1"},
            }
        ]
    }
    assert workflow_findings(flow) == ["long-lived-key:deploy"]


# ---- Part 3: detections -----------------------------------------------------


def fail(ts, ip, user):
    return {"ts": ts, "ip": ip, "user": user, "outcome": "failure"}


def ok(ts, ip, user):
    return {"ts": ts, "ip": ip, "user": user, "outcome": "success"}


def test_spraying_counts_distinct_users_not_attempts():
    same_user = [fail(i, "203.0.113.9", "ada") for i in range(8)]
    assert all(alert[0] != "spraying" for alert in detect(same_user))
    many = [fail(i * 10, "203.0.113.9", f"user{i}") for i in range(6)]
    assert detect(many) == [("spraying", "203.0.113.9", 40)]


def test_the_window_is_strict_and_slides():
    spread = [fail(i * 150, "198.51.100.7", f"u{i}") for i in range(5)]
    assert detect(spread) == []
    assert detect(spread, window=601) == [("spraying", "198.51.100.7", 600)]


def test_events_arrive_in_any_order():
    events = [fail(i * 10, "203.0.113.9", f"user{i}") for i in range(5)]
    assert detect(list(reversed(events))) == [("spraying", "203.0.113.9", 40)]


def test_brute_force_on_one_account_from_many_ips_fires_once():
    events = [fail(i, f"192.0.2.{i}", "ada") for i in range(15)]
    assert detect(events) == [("brute-force", "ada", 9)]


def test_a_success_after_failures_and_a_reset_after_success():
    events = [fail(1, "192.0.2.1", "bob"), fail(2, "192.0.2.2", "bob"), fail(3, "192.0.2.3", "bob"), ok(4, "192.0.2.3", "bob")]
    assert detect(events) == [("success-after-failures", "bob", 4)]
    two = [fail(1, "192.0.2.1", "eve"), fail(2, "192.0.2.1", "eve"), ok(3, "192.0.2.1", "eve")]
    assert detect(two) == []
    reset = [*two, fail(4, "192.0.2.1", "eve"), ok(5, "192.0.2.1", "eve")]
    assert detect(reset) == []
    old = [fail(1, "192.0.2.1", "max"), fail(2, "192.0.2.1", "max"), fail(3, "192.0.2.1", "max"), ok(700, "192.0.2.1", "max")]
    assert detect(old) == []


def test_alerts_are_sorted_by_time_then_kind():
    events = [fail(i, "203.0.113.9", f"user{i}") for i in range(5)]
    events += [fail(i, "192.0.2.50", "zed") for i in range(10)]
    assert detect(events) == [("spraying", "203.0.113.9", 4), ("brute-force", "zed", 9)]


def test_thresholds_are_parameters():
    events = [fail(1, "192.0.2.1", "a"), fail(2, "192.0.2.1", "b"), ok(3, "192.0.2.9", "a")]
    assert detect(events, spray_users=2, failures_before_success=1) == [
        ("spraying", "192.0.2.1", 2),
        ("success-after-failures", "a", 3),
    ]


# ---- Part 4: safe logging ---------------------------------------------------


def test_a_log_line_is_one_line_of_sorted_json():
    line = log_line({"user": "ada\n2026-11-26 login ok user=admin", "ip": "192.0.2.1", "event": "login"})
    assert "\n" not in line and "\r" not in line
    assert line.startswith('{"event":"login","ip":')
    assert json.loads(line)["user"] == "ada\n2026-11-26 login ok user=admin"


def test_secrets_are_redacted_at_any_depth_without_changing_the_event():
    event = {
        "user": "ada",
        "Password": "hunter2",
        "headers": {"Authorization": "Bearer abc", "X-Request-Id": "r1"},
        "attempts": [{"session_id": "s1", "ok": False}],
        "reset_token": "t0k",
    }
    parsed = json.loads(log_line(event))
    assert parsed == {
        "user": "ada",
        "Password": "[REDACTED]",
        "headers": {"Authorization": "[REDACTED]", "X-Request-Id": "r1"},
        "attempts": [{"session_id": "[REDACTED]", "ok": False}],
        "reset_token": "[REDACTED]",
    }
    assert event["Password"] == "hunter2"
    assert "hunter2" not in log_line(event) and "abc" not in log_line(event)


def test_non_ascii_is_escaped():
    line = log_line({"user": "joão‮"})
    assert line.isascii()
    assert json.loads(line) == {"user": "joão‮"}
