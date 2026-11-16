from pathlib import Path

import pytest

from dojo_aws import guard

HIDDEN = Path(__file__).parent / "fixtures"


def broken_rules(fixture):
    return sorted({failure.split(":")[0] for failure in guard("security.guard", HIDDEN / fixture)})


@pytest.mark.parametrize("fixture,rule", [
    ("unencrypted-bucket.yaml", "s3_buckets_are_private_and_encrypted"),
    ("rdp-range-from-internet.yaml", "no_ssh_or_rdp_from_the_internet"),
    ("all-traffic-from-internet.yaml", "no_ssh_or_rdp_from_the_internet"),
    ("public-database.yaml", "databases_are_encrypted_private_and_protected"),
    ("unprotected-database.yaml", "databases_are_encrypted_private_and_protected"),
    ("admin-role-in-a-list.yaml", "roles_grant_no_wildcard_admin"),
])
def test_catches_the_other_mistakes(fixture, rule):
    assert broken_rules(fixture) == [rule], f"{fixture} must break only {rule}"


def test_rules_skip_templates_without_their_resources():
    assert broken_rules("queue-only.yaml") == []


def test_every_failure_explains_itself():
    failures = guard("security.guard", HIDDEN / "public-database.yaml")
    assert failures and all("<<" not in f and len(f.split(":", 1)[1].strip()) > 0 for f in failures), (
        "add a <<message>> after each clause, so a failing team knows what to fix")


def test_keeps_the_five_rule_names():
    text = Path("security.guard").read_text(encoding="utf-8")
    for name in ["s3_buckets_are_private_and_encrypted", "no_ssh_or_rdp_from_the_internet",
                 "databases_are_encrypted_private_and_protected", "tables_have_point_in_time_recovery",
                 "roles_grant_no_wildcard_admin"]:
        assert f"rule {name}" in text, f"keep the rule {name}"
