from pathlib import Path

import pytest

from dojo_aws import guard

RULES = "rules/company.guard"
FIXTURES = Path(__file__).parent / "fixtures"
NAMES = ["imdsv2_only", "encrypted_volumes", "resilient_databases", "no_database_passwords", "rotating_keys",
         "https_only_listeners", "scoped_role_policies"]


def broken_rules(data):
    return sorted({failure.split(":")[0] for failure in guard(RULES, data)})


def test_keeps_the_seven_rule_names():
    text = Path(RULES).read_text(encoding="utf-8")
    for name in NAMES:
        assert f"rule {name}" in text, f"write the rule {name} in {RULES}"


def test_your_own_stack_passes():
    assert guard(RULES, "app.yaml") == [], f"app.yaml must pass your rules: {guard(RULES, 'app.yaml')}"


def test_a_compliant_stack_passes():
    assert broken_rules(FIXTURES / "compliant.json") == [], (
        f"a compliant stack must pass: {guard(RULES, FIXTURES / 'compliant.json')}")


def test_rules_skip_stacks_without_their_resources():
    assert broken_rules(FIXTURES / "queue-only.json") == [], "a rule with nothing to check must not fail"


@pytest.mark.parametrize("fixture,rule", [
    ("imds-optional.json", "imdsv2_only"),
    ("imds-default.json", "imdsv2_only"),
    ("unencrypted-data-volume.json", "encrypted_volumes"),
    ("single-az-database.json", "resilient_databases"),
    ("short-backups.json", "resilient_databases"),
    ("unprotected-database.json", "resilient_databases"),
    ("aws-managed-key.json", "resilient_databases"),
    ("database-password.json", "no_database_passwords"),
    ("key-without-rotation.json", "rotating_keys"),
    ("old-tls-policy.json", "https_only_listeners"),
    ("http-forwards.json", "https_only_listeners"),
    ("wildcard-service-action.json", "scoped_role_policies"),
    ("wildcard-action-in-a-list.json", "scoped_role_policies"),
    ("wildcard-resource.json", "scoped_role_policies"),
    ("wildcard-resource-alone.json", "scoped_role_policies"),
])
def test_each_mistake_breaks_exactly_its_rule(fixture, rule):
    found = broken_rules(FIXTURES / fixture)
    assert found == [rule], f"{fixture} must break only {rule}, it breaks {found}"


def test_every_failure_explains_itself():
    for fixture in ("single-az-database.json", "database-password.json", "http-forwards.json"):
        failures = guard(RULES, FIXTURES / fixture)
        assert failures and all("<<" not in f and f.split(":", 1)[1].strip() for f in failures), (
            "add a <<message>> after each clause, so a failing team knows what to fix")
