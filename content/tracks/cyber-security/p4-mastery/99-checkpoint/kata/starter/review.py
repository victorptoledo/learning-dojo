"""Post-incident review kit: threat model, evidence, timeline and remediation plan.

The first drafts below were written during the incident. Each one is wrong in a way the
README describes; rewrite them.
"""

import hashlib
from datetime import date, datetime, timedelta
from pathlib import Path


def stride(model):
    threats = []
    for name in model["elements"]:
        for letter in "STRIDE":
            threats.append((name, letter))
    for flow_id in model.get("flows", {}):
        for letter in "TID":
            threats.append((flow_id, letter))
    done = {tuple(item) for item in model.get("mitigations", [])}
    return sorted(threat for threat in threats if threat not in done)


def manifest(root):
    root = Path(root)
    return {path.name: hashlib.md5(path.read_bytes()).hexdigest() for path in root.iterdir() if path.is_file()}


def check(root, expected):
    actual = manifest(root)
    return {"modified": [name for name in expected if actual.get(name) != expected[name]], "missing": [], "extra": []}


def custody_append(chain, entry):
    chain.append({"entry": entry, "prev": "", "hash": hashlib.sha256(str(entry).encode()).hexdigest()})
    return chain


def custody_verify(chain):
    return None


def timeline(sources, iocs, aware_at):
    events = []
    for source in sources:
        for event in source["events"]:
            events.append({**event, "source": source["name"]})
    events.sort(key=lambda event: event["at"])
    hits = [event for event in events if event.get("ip") in iocs.get("ips", [])]
    first = hits[0]["at"] if hits else None
    notify = datetime.fromisoformat(first) + timedelta(hours=72) if first else None
    return {
        "events": events,
        "first_seen": first,
        "accounts": [event["user"] for event in hits],
        "notify_by": notify.isoformat() if notify else None,
    }


def remediation_plan(vulns, today):
    plan = []
    for vuln in vulns:
        priority = "P1" if vuln["cvss"] >= 9 else "P3"
        due = date.fromisoformat(vuln["found"]) + timedelta(days=30)
        status = "overdue" if date.fromisoformat(today) >= due else "open"
        plan.append({"id": vuln["id"], "priority": priority, "due": due.isoformat(), "status": status})
    return plan
