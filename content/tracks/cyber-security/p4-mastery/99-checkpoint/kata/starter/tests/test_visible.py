from review import custody_append, custody_verify, remediation_plan, stride, timeline


def test_a_data_store_cannot_be_spoofed():
    model = {"elements": {"api": "process", "db": "store"}, "flows": {"f1": ["api", "db"]}, "boundaries": []}
    assert stride(model) == [("api", "S"), ("api", "T"), ("api", "R"), ("api", "I"), ("api", "D"), ("api", "E"),
                             ("db", "T"), ("db", "I"), ("db", "D")]


def test_an_edited_custody_record_is_caught():
    chain = custody_append(custody_append([], {"who": "ana"}), {"who": "rui"})
    chain[0]["entry"]["who"] = "eve"
    assert custody_verify(chain) == 0


def test_naive_times_are_in_the_source_zone():
    sources = [{"name": "vpn", "tz": "America/Sao_Paulo", "events": [{"at": "2026-11-20 03:10:00", "ip": "203.0.113.66"}]}]
    result = timeline(sources, {"ips": ["203.0.113.66"]}, "2026-11-20T09:00:00-03:00")
    assert result["first_seen"] == "2026-11-20T06:10:00Z"


def test_a_known_exploited_internet_facing_bug_comes_first():
    vulns = [
        {"id": "CVE-A", "cvss": 9.8, "kev": False, "internet_facing": False, "found": "2026-11-01", "exception_until": None},
        {"id": "CVE-B", "cvss": 6.5, "kev": True, "internet_facing": True, "found": "2026-11-01", "exception_until": None},
    ]
    assert [(item["id"], item["priority"]) for item in remediation_plan(vulns, "2026-11-02")] == [("CVE-B", "P1"), ("CVE-A", "P2")]
