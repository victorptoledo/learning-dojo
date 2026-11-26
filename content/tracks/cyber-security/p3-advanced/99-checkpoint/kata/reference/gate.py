"""Release gate: cloud IAM, the CI workflow, detections and safe logging, checked before a deploy."""

import json
import re

ADMIN = "*"
SENSITIVE = ("password", "secret", "token", "authorization", "cookie", "session")
FULL_SHA = re.compile(r"[0-9a-fA-F]{40}")
DIGEST = re.compile(r"@sha256:[0-9a-f]{64}$")
UNTRUSTED_EXPR = re.compile(r"\$\{\{\s*(github\.event\.|github\.head_ref\b)")
UNTRUSTED_REF = re.compile(r"github\.event\.pull_request\.head|github\.head_ref")


# ---- Part 1: IAM ------------------------------------------------------------


def _as_list(value):
    if value is None:
        return []
    if isinstance(value, str):
        return [value]
    return list(value)


def _is_public(principal):
    if principal == "*":
        return True
    if isinstance(principal, dict):
        return "*" in _as_list(principal.get("AWS"))
    return False


def iam_findings(policy):
    statements = policy.get("Statement", [])
    if isinstance(statements, dict):
        statements = [statements]
    findings = []
    for index, statement in enumerate(statements):
        effect = statement.get("Effect")
        if effect not in ("Allow", "Deny"):
            raise ValueError(f"statement {index}: Effect must be Allow or Deny, got {effect!r}")
        if effect == "Deny":
            continue
        actions = [action.lower() for action in _as_list(statement.get("Action"))]
        resources = _as_list(statement.get("Resource"))
        if ADMIN in actions:
            findings.append(f"{index}:admin")
        elif any("*" in action for action in actions):
            findings.append(f"{index}:action-wildcard")
        if "NotAction" in statement:
            findings.append(f"{index}:not-action")
        if "iam:passrole" in actions and "*" in resources:
            findings.append(f"{index}:passrole-any")
        if _is_public(statement.get("Principal")) and not statement.get("Condition"):
            findings.append(f"{index}:public")
    return findings


# ---- Part 2: the CI workflow ------------------------------------------------


def _triggers(workflow):
    # PyYAML (YAML 1.1) reads the key `on` as the boolean True.
    on = workflow["on"] if "on" in workflow else workflow.get(True)
    if isinstance(on, dict):
        return set(on)
    return set(_as_list(on))


def _unpinned(uses):
    if uses.startswith("./"):
        return None
    if uses.startswith("docker://"):
        return None if DIGEST.search(uses) else uses.split("@", 1)[0]
    name, at, ref = uses.partition("@")
    if not at or not FULL_SHA.fullmatch(ref):
        return name
    return None


def workflow_findings(workflow):
    findings = set()
    if workflow.get("permissions") in (None, "write-all"):
        findings.add("token-permissions")
    target = "pull_request_target" in _triggers(workflow)
    for job_name, job in (workflow.get("jobs") or {}).items():
        if job.get("permissions") == "write-all":
            findings.add("token-permissions")
        for step in job.get("steps") or []:
            uses = step.get("uses")
            inputs = step.get("with") or {}
            if uses:
                name = _unpinned(uses)
                if name:
                    findings.add(f"unpinned:{name}")
                checkout = uses.split("@", 1)[0] == "actions/checkout"
                if target and checkout and UNTRUSTED_REF.search(str(inputs.get("ref", ""))):
                    findings.add("untrusted-checkout")
            if UNTRUSTED_EXPR.search(step.get("run") or ""):
                findings.add(f"script-injection:{job_name}")
            if any(str(key).lower() == "aws-access-key-id" for key in inputs):
                findings.add(f"long-lived-key:{job_name}")
    return sorted(findings)


# ---- Part 3: detections -----------------------------------------------------


def _recent(items, now, window):
    return [item for item in items if now - item[0] < window]


def detect(events, *, window=600, spray_users=5, brute_failures=10, failures_before_success=3):
    ordered = sorted(events, key=lambda event: event["ts"])
    alerts = []
    fired = set()
    failures_by_ip = {}
    failures_by_user = {}
    for event in ordered:
        now, ip, user = event["ts"], event["ip"], event["user"]
        if event["outcome"] == "failure":
            by_ip = _recent([*failures_by_ip.get(ip, []), (now, user)], now, window)
            failures_by_ip[ip] = by_ip
            by_user = _recent([*failures_by_user.get(user, []), (now, ip)], now, window)
            failures_by_user[user] = by_user
            if len({u for _, u in by_ip}) >= spray_users and ("spraying", ip) not in fired:
                fired.add(("spraying", ip))
                alerts.append(("spraying", ip, now))
            if len(by_user) >= brute_failures and ("brute-force", user) not in fired:
                fired.add(("brute-force", user))
                alerts.append(("brute-force", user, now))
        elif event["outcome"] == "success":
            before = _recent(failures_by_user.get(user, []), now, window)
            key = ("success-after-failures", user)
            if len(before) >= failures_before_success and key not in fired:
                fired.add(key)
                alerts.append(("success-after-failures", user, now))
            failures_by_user[user] = []
        else:
            raise ValueError(f"unknown outcome {event['outcome']!r}")
    return sorted(alerts, key=lambda alert: (alert[2], alert[0], alert[1]))


# ---- Part 4: safe logging ---------------------------------------------------


def _redact(value):
    if isinstance(value, dict):
        return {
            key: "[REDACTED]" if any(word in str(key).lower() for word in SENSITIVE) else _redact(item)
            for key, item in value.items()
        }
    if isinstance(value, list):
        return [_redact(item) for item in value]
    return value


def log_line(event):
    return json.dumps(_redact(event), sort_keys=True, ensure_ascii=True, separators=(",", ":"))
