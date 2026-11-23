"""What app.yaml costs a month, before anyone deploys it."""

from dojo_aws.stack import Stack


def monthly_cost(template: dict, parameters: dict, prices: dict) -> dict:
    """{"lines": {logical id: dollars}, "total": dollars}, rounded to cents.

    `template` is app.yaml already parsed (intrinsics in their long form, {"Ref": "X"});
    `parameters` overrides the template's parameter defaults; `prices` is like prices.json.
    """
    stack = Stack(template, parameters)
    hours = prices["hours_per_month"]
    lines = {}
    for name, group in stack.resources_of("AWS::AutoScaling::AutoScalingGroup").items():
        spec = group["Properties"]
        launch = stack.props(spec["LaunchTemplate"]["LaunchTemplateId"]["Ref"])["LaunchTemplateData"]
        instance = prices["ec2_hour"][stack.value(launch["InstanceType"])] * hours
        volumes = sum(int(m["Ebs"].get("VolumeSize", 8)) for m in launch.get("BlockDeviceMappings", []) if "Ebs" in m)
        lines[name] = int(stack.value(spec["MinSize"])) * (instance + volumes * prices["ebs_gp3_gb_month"])
    for name, database in stack.resources_of("AWS::RDS::DBInstance").items():
        spec = database["Properties"]
        copies = 2 if stack.value(spec.get("MultiAZ", False)) is True else 1
        cost = copies * (prices["rds_hour"][stack.value(spec["DBInstanceClass"])] * hours
                         + int(stack.value(spec["AllocatedStorage"])) * prices["rds_gp3_gb_month"])
        if spec.get("ManageMasterUserPassword") is True:
            cost += prices["secret_month"]
        lines[name] = cost
    for name in stack.resources_of("AWS::ElasticLoadBalancingV2::LoadBalancer"):
        lines[name] = prices["alb_hour"] * hours
    for name in stack.resources_of("AWS::KMS::Key"):
        lines[name] = prices["kms_key_month"]
    return {"lines": {name: round(cost, 2) for name, cost in lines.items()},
            "total": round(sum(lines.values()), 2)}


def fits_budget(template: dict, parameters: dict, prices: dict) -> bool:
    """Whether the estimate is within the template's MonthlyBudget."""
    budget = float(Stack(template, parameters).parameters["MonthlyBudget"])
    return monthly_cost(template, parameters, prices)["total"] <= budget
