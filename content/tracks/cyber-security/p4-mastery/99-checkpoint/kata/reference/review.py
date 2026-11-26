"""Post-incident review kit: threat model, evidence, timeline and remediation plan."""

import hashlib
import json
from datetime import UTC, date, datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

STRIDE = "STRIDE"
THREATS = {"external": "SR", "process": "STRIDE", "store": "TID", "log": "TRID"}
FLOW_THREATS = "TID"
GENESIS = "0" * 64
NOTIFY_WITHIN = timedelta(hours=72)
SLAS = {"P1": 3, "P2": 14, "P3": 30, "P4": 90, "P5": 180}
STATUS_ORDER = {"overdue": 0, "open": 1, "excepted": 2}


# ---- Part 1: STRIDE per element ---------------------------------------------


def stride(model):
    elements = model["elements"]
    flows = model.get("flows", {})
    boundaries = [set(boundary) for boundary in model.get("boundaries", [])]
    for name, kind in elements.items():
        if kind not in THREATS:
            raise ValueError(f"{name}: unknown kind {kind!r}")
    for boundary in boundaries:
        if not boundary <= set(elements):
            raise ValueError(f"boundary names unknown elements: {sorted(boundary - set(elements))}")
    threats = {name: THREATS[kind] for name, kind in elements.items()}
    for flow_id, (source, target) in flows.items():
        if flow_id in elements:
            raise ValueError(f"{flow_id}: a flow and an element share a name")
        if source not in elements or target not in elements:
            raise ValueError(f"{flow_id}: unknown endpoint")
        if "process" not in (elements[source], elements[target]):
            raise ValueError(f"{flow_id}: data must pass through a process")
        crosses = any((source in boundary) != (target in boundary) for boundary in boundaries)
        threats[flow_id] = FLOW_THREATS if crosses else ""
    mitigated = set()
    for name, letter in model.get("mitigations", []):
        if name not in threats or letter not in threats[name]:
            raise ValueError(f"{name}/{letter}: mitigates a threat that is not in the model")
        mitigated.add((name, letter))
    open_threats = [
        (name, letter) for name, letters in threats.items() for letter in letters if (name, letter) not in mitigated
    ]
    return sorted(open_threats, key=lambda threat: (threat[0], STRIDE.index(threat[1])))


# ---- Part 2: evidence and chain of custody ----------------------------------


def manifest(root):
    root = Path(root)
    result = {}
    for path in sorted(root.rglob("*")):
        if path.is_file() and not path.is_symlink():
            with path.open("rb") as handle:
                result[path.relative_to(root).as_posix()] = hashlib.file_digest(handle, "sha256").hexdigest()
    return result


def check(root, expected):
    actual = manifest(root)
    return {
        "modified": sorted(name for name in expected if name in actual and actual[name] != expected[name].lower()),
        "missing": sorted(name for name in expected if name not in actual),
        "extra": sorted(name for name in actual if name not in expected),
    }


def _record_hash(prev, entry):
    canonical = json.dumps(entry, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    return hashlib.sha256((prev + canonical).encode("utf-8")).hexdigest()


def custody_append(chain, entry):
    prev = chain[-1]["hash"] if chain else GENESIS
    return [*chain, {"entry": entry, "prev": prev, "hash": _record_hash(prev, entry)}]


def custody_verify(chain):
    prev = GENESIS
    for index, record in enumerate(chain):
        if record["prev"] != prev or record["hash"] != _record_hash(prev, record["entry"]):
            return index
        prev = record["hash"]
    return None


# ---- Part 3: timeline and scope ---------------------------------------------


def _utc(dt):
    return dt.astimezone(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")


def timeline(sources, iocs, aware_at):
    aware = datetime.fromisoformat(aware_at)
    if aware.tzinfo is None:
        raise ValueError("aware_at needs a UTC offset")
    bad_ips = set(iocs.get("ips", []))
    bad_hashes = {value.lower() for value in iocs.get("hashes", [])}
    rows = []
    for source in sources:
        zone = ZoneInfo(source["tz"])
        for index, event in enumerate(source["events"]):
            moment = datetime.fromisoformat(event["at"])
            if moment.tzinfo is None:
                moment = moment.replace(tzinfo=zone)
            moment = moment.astimezone(UTC)
            hit = event.get("ip") in bad_ips or str(event.get("sha256", "")).lower() in bad_hashes
            rows.append((moment, source["name"], index, hit, event))
    rows.sort(key=lambda row: row[:3])
    events = [{**event, "at": _utc(moment), "source": name} for moment, name, _, _, event in rows]
    hits = [row for row in rows if row[3]]
    return {
        "events": events,
        "first_seen": _utc(hits[0][0]) if hits else None,
        "accounts": sorted({row[4]["user"] for row in hits if row[4].get("user")}),
        "notify_by": _utc(aware + NOTIFY_WITHIN),
    }


# ---- Part 4: remediation plan -----------------------------------------------


def _priority(vuln):
    cvss, kev, exposed = vuln["cvss"], vuln["kev"], vuln["internet_facing"]
    if isinstance(cvss, bool) or not isinstance(cvss, int | float) or not 0 <= cvss <= 10:
        raise ValueError(f"{vuln['id']}: CVSS must be a number from 0 to 10")
    if type(kev) is not bool or type(exposed) is not bool:
        raise ValueError(f"{vuln['id']}: kev and internet_facing must be booleans")
    if kev and exposed:
        return "P1"
    if kev or cvss >= 9.0:
        return "P2"
    if cvss >= 7.0:
        return "P3"
    if cvss >= 4.0:
        return "P4"
    return "P5"


def remediation_plan(vulns, today):
    today = date.fromisoformat(today)
    plan = []
    for vuln in vulns:
        priority = _priority(vuln)
        due = date.fromisoformat(vuln["found"]) + timedelta(days=SLAS[priority])
        until = vuln.get("exception_until")
        if until and date.fromisoformat(until) >= today:
            status = "excepted"
        elif today > due:
            status = "overdue"
        else:
            status = "open"
        plan.append({"id": vuln["id"], "priority": priority, "due": due.isoformat(), "status": status})
    return sorted(plan, key=lambda item: (STATUS_ORDER[item["status"]], item["due"], item["id"]))
