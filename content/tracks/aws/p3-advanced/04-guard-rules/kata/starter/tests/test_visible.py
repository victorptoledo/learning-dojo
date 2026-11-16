from pathlib import Path

import pytest

from dojo_aws import guard

FIXTURES = Path(__file__).parent / "fixtures"


def broken_rules(fixture):
    return sorted({failure.split(":")[0] for failure in guard("security.guard", FIXTURES / fixture)})


def test_a_compliant_template_passes_every_rule():
    assert broken_rules("compliant.yaml") == []


@pytest.mark.parametrize("fixture,rule", [
    ("public-bucket.yaml", "s3_buckets_are_private_and_encrypted"),
    ("ssh-from-internet.yaml", "no_ssh_or_rdp_from_the_internet"),
    ("unencrypted-database.yaml", "databases_are_encrypted_private_and_protected"),
    ("table-without-pitr.yaml", "tables_have_point_in_time_recovery"),
    ("admin-role.yaml", "roles_grant_no_wildcard_admin"),
])
def test_each_mistake_breaks_exactly_its_rule(fixture, rule):
    assert broken_rules(fixture) == [rule]
