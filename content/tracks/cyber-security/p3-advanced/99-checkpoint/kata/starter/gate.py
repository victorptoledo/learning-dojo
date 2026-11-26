"""Release gate: cloud IAM, the CI workflow, detections and safe logging, checked before a deploy.

Each check below was written in a hurry and misses most of what the README asks for.
"""


def iam_findings(policy):
    findings = []
    for index, statement in enumerate(policy["Statement"]):
        if statement["Action"] == "*":
            findings.append(f"{index}:admin")
    return findings


def workflow_findings(workflow):
    findings = []
    for job in workflow["jobs"].values():
        for step in job["steps"]:
            uses = step.get("uses", "")
            if uses and "@" not in uses:
                findings.append(f"unpinned:{uses}")
    return findings


def detect(events, *, window=600, spray_users=5, brute_failures=10, failures_before_success=3):
    alerts = []
    failures = {}
    for event in events:
        if event["outcome"] == "failure":
            failures[event["ip"]] = failures.get(event["ip"], 0) + 1
            if failures[event["ip"]] == spray_users:
                alerts.append(("spraying", event["ip"], event["ts"]))
    return alerts


def log_line(event):
    return " ".join(f"{key}={value}" for key, value in event.items())
