from pathlib import Path

from dojo_aws import guard

RULES = Path(__file__).parent / "company.guard"


def test_the_company_rules_pass():
    assert guard(RULES, "service.yaml") == []
