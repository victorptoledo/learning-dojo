import copy
import json
from pathlib import Path

import pytest

from dojo_aws import guard, iam
from dojo_aws.stack import ACCOUNT
from webapp import DEPLOYMENT, props, stack, tpl

TAGGED = ["AWS::EC2::SecurityGroup", "AWS::ElasticLoadBalancingV2::LoadBalancer",
          "AWS::ElasticLoadBalancingV2::TargetGroup", "AWS::RDS::DBInstance", "AWS::KMS::Key",
          "AWS::Logs::LogGroup", "AWS::IAM::Role", "AWS::SNS::Topic"]
TOPIC = f"arn:aws:sns:eu-west-1:{ACCOUNT}:dojo-AlertTopic"
PRICES = {
    "currency": "USD", "hours_per_month": 730,
    "ec2_hour": {"t4g.small": 0.0168, "t4g.medium": 0.0336, "m7g.large": 0.0816},
    "ebs_gp3_gb_month": 0.088,
    "rds_hour": {"db.t4g.medium": 0.072, "db.m7g.large": 0.188},
    "rds_gp3_gb_month": 0.137, "alb_hour": 0.0252, "kms_key_month": 1.0, "secret_month": 0.4,
}
OTHER_PRICES = {
    "currency": "USD", "hours_per_month": 744,
    "ec2_hour": {"t4g.small": 0.02, "t4g.medium": 0.04, "m7g.large": 0.1},
    "ebs_gp3_gb_month": 0.1,
    "rds_hour": {"db.t4g.medium": 0.1, "db.m7g.large": 0.2},
    "rds_gp3_gb_month": 0.2, "alb_hour": 0.03, "kms_key_month": 1.5, "secret_month": 0.5,
}


def cost(template=None, prices=PRICES, **overrides):
    from cost import monthly_cost

    template = template or tpl()
    declared = template.get("Parameters") or {}
    parameters = {k: v for k, v in {**DEPLOYMENT, **overrides}.items() if k in declared}
    return monthly_cost(copy.deepcopy(template), parameters, prices)


def tags_of(res):
    return {tag.get("Key"): tag for tag in (res.get("Properties") or {}).get("Tags", [])}


def test_the_cost_parameters():
    found = tpl().get("Parameters", {})
    assert found.get("CostCenter", {}).get("AllowedPattern") == r"^CC-\d{4}$", r"CostCenter: AllowedPattern ^CC-\d{4}$"
    assert found.get("MonthlyBudget", {}).get("Type") == "Number", "MonthlyBudget: Type Number"
    assert str(found.get("MonthlyBudget", {}).get("Default")) == "250", "MonthlyBudget defaults to 250"
    assert "AlertEmail" in found, "declare the parameter AlertEmail"


@pytest.mark.parametrize("type_", TAGGED)
def test_every_billable_resource_carries_the_cost_center(type_):
    found = stack().resources_of(type_)
    assert found, f"the stack should have a {type_}"
    for name, res in found.items():
        tag = tags_of(res).get("cost-center")
        assert tag and stack().value(tag.get("Value")) == "CC-1234", f"{name} needs the tag cost-center = !Ref CostCenter"


def test_instances_inherit_the_cost_center():
    group = stack().resources_of("AWS::AutoScaling::AutoScalingGroup")["AutoScalingGroup"]
    tag = tags_of(group).get("cost-center")
    assert tag and stack().value(tag.get("Value")) == "CC-1234", "tag the Auto Scaling group with cost-center"
    assert tag.get("PropagateAtLaunch") is True, "PropagateAtLaunch: true, so every instance is tagged"


def test_alerts_go_to_finance_by_email():
    spec = props("AWS::SNS::Topic", "AlertTopic")
    subscriptions = stack().value(spec.get("Subscription", []))
    assert {"Protocol": "email", "Endpoint": "finance@example.com"} in subscriptions, (
        "AlertTopic needs an email Subscription to !Ref AlertEmail")


@pytest.fixture(scope="module")
def topic_policy():
    spec = stack().value(props("AWS::SNS::TopicPolicy", "AlertTopicPolicy"))
    assert spec.get("Topics") == [TOPIC], "AlertTopicPolicy: Topics [!Ref AlertTopic]"
    policy = spec.get("PolicyDocument")
    assert iam.validate(policy, "resource") == [], "; ".join(iam.validate(policy, "resource"))
    return policy


@pytest.mark.parametrize("case,principal,action,account,expected", [
    ("budgets of this account publish", "budgets.amazonaws.com", "sns:Publish", ACCOUNT, iam.ALLOW),
    ("budgets for another account", "budgets.amazonaws.com", "sns:Publish", "444455556666", iam.IMPLICIT_DENY),
    ("budgets with no source account", "budgets.amazonaws.com", "sns:Publish", None, iam.IMPLICIT_DENY),
    ("budgets changes the topic", "budgets.amazonaws.com", "sns:SetTopicAttributes", ACCOUNT, iam.IMPLICIT_DENY),
    ("another service publishes", "events.amazonaws.com", "sns:Publish", ACCOUNT, iam.IMPLICIT_DENY),
    ("someone outside publishes", "arn:aws:iam::444455556666:user/eve", "sns:Publish", None, iam.IMPLICIT_DENY),
])
def test_only_budgets_of_this_account_publish(topic_policy, case, principal, action, account, expected):
    context = {"aws:SourceAccount": account} if account else {}
    request = iam.Request(principal=principal, action=action, resource=TOPIC, context=context)
    found = iam.evaluate(request, resource_policy=topic_policy)
    assert found == expected, f"{case}: expected {expected}, the topic policy gives {found}"


def test_the_budget_watches_this_cost_center_every_month():
    spec = stack().value(props("AWS::Budgets::Budget", "MonthlyCost")).get("Budget", {})
    assert spec.get("BudgetType") == "COST" and spec.get("TimeUnit") == "MONTHLY", "a MONTHLY COST budget"
    limit = spec.get("BudgetLimit", {})
    assert float(limit.get("Amount", 0)) == 250 and limit.get("Unit") == "USD", "BudgetLimit: !Ref MonthlyBudget USD"
    assert spec.get("CostFilters") == {"TagKeyValue": ["user:cost-center$CC-1234"]}, (
        "filter the budget to the cost center: TagKeyValue user:cost-center$<CostCenter>")


def test_the_budget_warns_before_the_money_is_gone():
    spec = stack().value(props("AWS::Budgets::Budget", "MonthlyCost"))
    found = set()
    for entry in spec.get("NotificationsWithSubscribers", []):
        notice = entry.get("Notification", {})
        assert notice.get("ComparisonOperator") == "GREATER_THAN"
        assert notice.get("ThresholdType", "PERCENTAGE") == "PERCENTAGE"
        assert {"SubscriptionType": "SNS", "Address": TOPIC} in entry.get("Subscribers", []), (
            "every notification goes to !Ref AlertTopic")
        found.add((notice.get("NotificationType"), float(notice.get("Threshold"))))
    assert found == {("ACTUAL", 80.0), ("FORECASTED", 100.0)}, f"notify at 80% actual and 100% forecasted, found {found}"


def test_the_stack_still_follows_the_company_rules():
    assert guard("rules/company.guard", "app.yaml") == []


def test_the_default_stack_costs_what_finance_expects():
    found = cost()
    expected = {"AutoScalingGroup": 42.07, "Database": 119.22, "LoadBalancer": 18.4, "DataKey": 1.0}
    assert set(found) == {"lines", "total"}, f"return {{'lines': ..., 'total': ...}}, got {found}"
    assert found["lines"] == pytest.approx(expected, abs=0.011), f"lines: {found['lines']}, expected {expected}"
    assert found["total"] == pytest.approx(180.69, abs=0.011), f"total: {found['total']}, expected 180.69"


def test_bigger_sizes_come_from_the_parameters():
    found = cost(InstanceType="m7g.large", DatabaseClass="db.m7g.large")
    assert found["lines"]["AutoScalingGroup"] == pytest.approx(183.98, abs=0.011), found["lines"]
    assert found["lines"]["Database"] == pytest.approx(288.58, abs=0.011), found["lines"]
    assert found["total"] == pytest.approx(491.96, abs=0.011), found["total"]


def test_prices_come_from_the_table():
    found = cost(prices=OTHER_PRICES)
    assert found["lines"]["AutoScalingGroup"] == pytest.approx(3 * (0.02 * 744 + 20 * 0.1), abs=0.011)
    assert found["lines"]["Database"] == pytest.approx(2 * (0.1 * 744 + 50 * 0.2) + 0.5, abs=0.011)
    assert found["lines"]["LoadBalancer"] == pytest.approx(0.03 * 744, abs=0.011)
    assert found["lines"]["DataKey"] == pytest.approx(1.5, abs=0.011)


def test_sizes_and_counts_come_from_the_template():
    changed = copy.deepcopy(tpl())
    resources = changed["Resources"]
    resources["AutoScalingGroup"]["Properties"]["MinSize"] = "6"
    resources["LaunchTemplate"]["Properties"]["LaunchTemplateData"]["BlockDeviceMappings"][0]["Ebs"]["VolumeSize"] = 50
    resources["Database"]["Properties"]["MultiAZ"] = False
    resources["Database"]["Properties"]["AllocatedStorage"] = "100"
    found = cost(template=changed)
    assert found["lines"]["AutoScalingGroup"] == pytest.approx(6 * (0.0168 * 730 + 50 * 0.088), abs=0.011), (
        "read MinSize and the volume sizes from the template")
    assert found["lines"]["Database"] == pytest.approx(0.072 * 730 + 100 * 0.137 + 0.4, abs=0.011), (
        "a single-AZ database costs one copy")


def test_the_prices_file_has_every_price_used():
    shipped = json.loads(Path("prices.json").read_text(encoding="utf-8"))
    assert set(PRICES) <= set(shipped), "keep every key of prices.json"


def test_the_default_stack_fits_the_budget_and_the_big_one_does_not():
    from cost import fits_budget

    declared = tpl().get("Parameters") or {}
    base = {k: v for k, v in DEPLOYMENT.items() if k in declared}
    assert fits_budget(copy.deepcopy(tpl()), base, PRICES) is True, "181 dollars fit a 250 dollar budget"
    big = {**base, "InstanceType": "m7g.large", "DatabaseClass": "db.m7g.large"}
    assert fits_budget(copy.deepcopy(tpl()), big, PRICES) is False, "492 dollars do not fit a 250 dollar budget"
    assert fits_budget(copy.deepcopy(tpl()), {**big, "MonthlyBudget": "600"}, PRICES) is True, (
        "read MonthlyBudget from the parameters")


def test_the_cost_line_names_are_logical_ids():
    found = cost()
    assert all(name in tpl()["Resources"] for name in found["lines"]), "key the lines by logical id"
