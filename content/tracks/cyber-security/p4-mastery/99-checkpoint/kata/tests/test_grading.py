import copy
import hashlib

import pytest

from review import check, custody_append, custody_verify, manifest, remediation_plan, stride, timeline

# ---- Part 1: STRIDE per element ---------------------------------------------


def share_link_model(**changes):
    model = {
        "elements": {"user": "external", "api": "process", "docs": "store", "audit": "log"},
        "flows": {"f1": ["user", "api"], "f2": ["api", "docs"], "f3": ["api", "audit"]},
        "boundaries": [["api", "docs", "audit"]],
        "mitigations": [],
    }
    model.update(changes)
    return model


def test_threats_per_element_and_only_on_flows_that_cross_a_boundary():
    assert stride(share_link_model()) == [
        ("api", "S"), ("api", "T"), ("api", "R"), ("api", "I"), ("api", "D"), ("api", "E"),
        ("audit", "T"), ("audit", "R"), ("audit", "I"), ("audit", "D"),
        ("docs", "T"), ("docs", "I"), ("docs", "D"),
        ("f1", "T"), ("f1", "I"), ("f1", "D"),
        ("user", "S"), ("user", "R"),
    ]


def test_letters_follow_stride_order_not_the_alphabet():
    threats = [letter for name, letter in stride(share_link_model()) if name == "api"]
    assert "".join(threats) == "STRIDE"


def test_mitigations_remove_threats():
    mitigations = [["f1", "T"], ["f1", "I"], ["user", "S"], ["api", "E"], ["docs", "I"]]
    remaining = stride(share_link_model(mitigations=mitigations))
    assert ("f1", "T") not in remaining and ("api", "E") not in remaining
    assert ("f1", "D") in remaining and ("user", "R") in remaining
    assert len(remaining) == 18 - 5


def test_any_boundary_counts_and_a_flow_inside_two_boundaries_does_not_cross():
    model = share_link_model(boundaries=[["api", "docs", "audit"], ["docs"]])
    assert [letter for name, letter in stride(model) if name == "f2"] == ["T", "I", "D"]
    model = share_link_model(boundaries=[])
    assert all(not name.startswith("f") for name, _ in stride(model))


def test_a_mitigation_for_a_threat_that_does_not_exist_is_an_error():
    for wrong in (["docs", "S"], ["f2", "T"], ["user", "E"], ["nope", "T"], ["api", "X"]):
        with pytest.raises(ValueError):
            stride(share_link_model(mitigations=[wrong]))


def test_invalid_diagrams_are_rejected():
    with pytest.raises(ValueError):
        stride(share_link_model(flows={"f1": ["user", "docs"]}))
    with pytest.raises(ValueError):
        stride(share_link_model(flows={"f1": ["user", "ghost"]}))
    with pytest.raises(ValueError):
        stride(share_link_model(flows={"api": ["user", "api"]}))
    with pytest.raises(ValueError):
        stride(share_link_model(elements={"user": "human", "api": "process"}, flows={}, boundaries=[]))
    with pytest.raises(ValueError):
        stride(share_link_model(boundaries=[["api", "cache"]]))


# ---- Part 2: evidence and chain of custody ----------------------------------


def sha(data):
    return hashlib.sha256(data).hexdigest()


def evidence(tmp_path):
    (tmp_path / "logs").mkdir()
    (tmp_path / "logs" / "auth.log").write_bytes(b"line 1\nline 2\n")
    (tmp_path / "memory.raw").write_bytes(bytes(range(256)) * 5000)
    return tmp_path


def test_manifest_hashes_every_file_with_relative_posix_paths(tmp_path):
    root = evidence(tmp_path)
    assert manifest(root) == {
        "logs/auth.log": sha(b"line 1\nline 2\n"),
        "memory.raw": sha(bytes(range(256)) * 5000),
    }
    assert manifest(str(root)) == manifest(root)


def test_check_reports_modified_missing_and_extra(tmp_path):
    root = evidence(tmp_path)
    expected = manifest(root)
    expected["logs/auth.log"] = expected["logs/auth.log"].upper()
    assert check(root, expected) == {"modified": [], "missing": [], "extra": []}
    (root / "logs" / "auth.log").write_bytes(b"line 1\n")
    (root / "memory.raw").unlink()
    (root / "logs" / "notes.txt").write_text("collected by ana")
    assert check(root, expected) == {"modified": ["logs/auth.log"], "missing": ["memory.raw"], "extra": ["logs/notes.txt"]}


def test_custody_records_are_hash_chained():
    first = {"who": "ana", "what": "memory.raw", "when": "2026-11-20T07:00:00Z", "action": "collected"}
    chain = custody_append([], first)
    assert chain[0]["prev"] == "0" * 64
    assert chain[0]["hash"] == sha(("0" * 64 + '{"action":"collected","what":"memory.raw","when":"2026-11-20T07:00:00Z","who":"ana"}').encode())
    longer = custody_append(chain, {"who": "joão", "action": "transferred"})
    assert len(chain) == 1 and len(longer) == 2
    assert longer[1]["prev"] == chain[0]["hash"]
    assert longer[1]["hash"] == sha((chain[0]["hash"] + '{"action":"transferred","who":"joão"}').encode())
    assert custody_verify(longer) is None
    assert custody_verify([]) is None


def test_custody_verify_finds_the_first_broken_record():
    chain = []
    for step in range(4):
        chain = custody_append(chain, {"step": step})
    edited = copy.deepcopy(chain)
    edited[1]["entry"]["step"] = 99
    assert custody_verify(edited) == 1
    rehashed = copy.deepcopy(edited)
    rehashed[1]["hash"] = sha((rehashed[1]["prev"] + '{"step":99}').encode())
    assert custody_verify(rehashed) == 2
    assert custody_verify([chain[0], chain[2], chain[3]]) == 1
    assert custody_verify(chain[1:]) == 0
    relinked = copy.deepcopy(chain)
    relinked[2]["prev"] = "f" * 64
    assert custody_verify(relinked) == 2


# ---- Part 3: timeline and scope ---------------------------------------------

SOURCES = [
    {
        "name": "vpn",
        "tz": "America/Sao_Paulo",
        "events": [
            {"at": "2026-11-20 03:10:00", "ip": "203.0.113.66", "user": "bia", "what": "login"},
            {"at": "2026-11-20 03:12:00", "ip": "192.0.2.10", "user": "caio", "what": "login"},
        ],
    },
    {
        "name": "app",
        "tz": "UTC",
        "events": [
            {"at": "2026-11-20T06:05:00", "ip": "192.0.2.10", "user": "caio", "what": "export"},
            {"at": "2026-11-20T08:11:00+02:00", "ip": "192.0.2.20", "user": "dani", "what": "upload", "sha256": "AB" * 32},
            {"at": "2026-11-20T06:12:00Z", "ip": "192.0.2.30", "user": "", "what": "health"},
        ],
    },
]
IOCS = {"ips": ["203.0.113.66"], "hashes": ["ab" * 32]}


def test_events_are_merged_in_utc_order_with_naive_times_in_the_source_zone():
    result = timeline(SOURCES, IOCS, "2026-11-20T09:00:00-03:00")
    assert [(event["at"], event["source"], event["what"]) for event in result["events"]] == [
        ("2026-11-20T06:05:00Z", "app", "export"),
        ("2026-11-20T06:10:00Z", "vpn", "login"),
        ("2026-11-20T06:11:00Z", "app", "upload"),
        ("2026-11-20T06:12:00Z", "app", "health"),
        ("2026-11-20T06:12:00Z", "vpn", "login"),
    ]
    assert result["events"][1] == {"at": "2026-11-20T06:10:00Z", "source": "vpn", "ip": "203.0.113.66", "user": "bia", "what": "login"}


def test_first_seen_accounts_and_the_hash_ioc_ignore_case():
    result = timeline(SOURCES, IOCS, "2026-11-20T09:00:00-03:00")
    assert result["first_seen"] == "2026-11-20T06:10:00Z"
    assert result["accounts"] == ["bia", "dani"]
    nothing = timeline(SOURCES, {"ips": ["198.51.100.1"]}, "2026-11-20T12:00:00+00:00")
    assert nothing["first_seen"] is None and nothing["accounts"] == []


def test_the_notification_clock_starts_when_you_became_aware():
    result = timeline(SOURCES, IOCS, "2026-11-21T10:30:00-03:00")
    assert result["notify_by"] == "2026-11-24T13:30:00Z"
    with pytest.raises(ValueError):
        timeline(SOURCES, IOCS, "2026-11-21T10:30:00")


def test_the_sources_are_not_changed():
    before = copy.deepcopy(SOURCES)
    timeline(SOURCES, IOCS, "2026-11-20T09:00:00+00:00")
    assert before == SOURCES


# ---- Part 4: remediation plan -----------------------------------------------


def vuln(id, cvss, kev=False, internet_facing=False, found="2026-11-01", exception_until=None):
    return {"id": id, "cvss": cvss, "kev": kev, "internet_facing": internet_facing, "found": found, "exception_until": exception_until}


def plan_of(*vulns, today="2026-11-02"):
    return {item["id"]: (item["priority"], item["due"], item["status"]) for item in remediation_plan(list(vulns), today)}


def test_priorities_use_kev_and_exposure_before_cvss():
    plan = plan_of(
        vuln("kev-web", 6.5, kev=True, internet_facing=True),
        vuln("kev-internal", 5.0, kev=True),
        vuln("critical-internal", 9.8),
        vuln("critical-boundary", 9.0, internet_facing=True),
        vuln("high", 7.0),
        vuln("high-below", 6.9),
        vuln("medium", 4.0),
        vuln("low", 3.9),
        vuln("zero", 0),
    )
    assert {key: value[0] for key, value in plan.items()} == {
        "kev-web": "P1", "kev-internal": "P2", "critical-internal": "P2", "critical-boundary": "P2",
        "high": "P3", "high-below": "P4", "medium": "P4", "low": "P5", "zero": "P5",
    }


def test_due_dates_and_the_last_day_is_not_overdue():
    assert plan_of(vuln("a", 6.5, kev=True, internet_facing=True), today="2026-11-04")["a"] == ("P1", "2026-11-04", "open")
    assert plan_of(vuln("a", 6.5, kev=True, internet_facing=True), today="2026-11-05")["a"] == ("P1", "2026-11-04", "overdue")
    assert plan_of(vuln("b", 9.1, found="2026-12-20"))["b"] == ("P2", "2027-01-03", "open")
    assert plan_of(vuln("c", 2.0, found="2026-06-01"), today="2026-11-28")["c"] == ("P5", "2026-11-28", "open")


def test_exceptions_hold_until_their_last_day_and_then_expire():
    excepted = vuln("x", 9.5, found="2026-01-01", exception_until="2026-11-30")
    assert plan_of(excepted, today="2026-11-30")["x"][2] == "excepted"
    assert plan_of(excepted, today="2026-12-01")["x"][2] == "overdue"
    early = vuln("y", 5.0, exception_until="2026-11-10")
    assert plan_of(early, today="2026-11-03")["y"][2] == "excepted"


def test_the_plan_is_ordered_overdue_then_open_then_excepted():
    items = remediation_plan(
        [
            vuln("e2", 9.9, found="2026-01-01", exception_until="2027-01-01"),
            vuln("o2", 5.0, found="2026-11-01"),
            vuln("late-b", 9.0, found="2026-10-01"),
            vuln("late-a", 9.0, found="2026-10-01"),
            vuln("o1", 7.5, found="2026-11-01"),
            vuln("late-c", 6.0, kev=True, internet_facing=True, found="2026-10-20"),
        ],
        "2026-11-02",
    )
    assert [item["id"] for item in items] == ["late-a", "late-b", "late-c", "o1", "o2", "e2"]


def test_bad_scores_fail_loudly():
    for bad in (vuln("a", 10.1), vuln("b", -1), vuln("c", True), vuln("d", "9.8"), vuln("e", 5.0, kev="yes")):
        with pytest.raises(ValueError):
            remediation_plan([bad], "2026-11-02")
