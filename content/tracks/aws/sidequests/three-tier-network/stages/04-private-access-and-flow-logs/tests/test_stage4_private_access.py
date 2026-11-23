import pytest

from dojo_aws import iam
from dojo_aws.stack import ACCOUNT
from network import ZONES, ingress, props, route_table_of, stack

INTERFACES = {"SsmEndpoint": "ssm", "SsmMessagesEndpoint": "ssmmessages", "Ec2MessagesEndpoint": "ec2messages"}
ROLE = f"arn:aws:iam::{ACCOUNT}:role/dojo-FlowLogRole"
GROUP = f"arn:aws:logs:eu-west-1:{ACCOUNT}:log-group:dojo-FlowLogGroup"
FLOW_LOGS = "vpc-flow-logs.amazonaws.com"


def endpoint(found, name):
    assert name in found.resources_of("AWS::EC2::VPCEndpoint"), f"declare the VPC endpoint {name}"
    return props(found, name)


def test_endpoints_are_reached_only_by_the_app_tier():
    found = ingress(stack(), "EndpointSecurityGroup")
    expected = [{"protocol": "tcp", "ports": (443, 443), "group": "AppSecurityGroup"}]
    assert found == expected, f"EndpointSecurityGroup ingress must be {expected}, found {found}"


@pytest.mark.parametrize("name,service", INTERFACES.items())
@pytest.mark.parametrize("environment,subnets", [
    ("prod", ["dojo-AppSubnetA", "dojo-AppSubnetB", "dojo-AppSubnetC"]),
    ("dev", ["dojo-AppSubnetA"]),
])
def test_session_manager_is_reached_through_interface_endpoints(name, service, environment, subnets):
    found = stack(Environment=environment)
    spec = endpoint(found, name)
    assert spec.get("VpcEndpointType") == "Interface", f"{name}: VpcEndpointType must be Interface"
    assert found.value(spec.get("ServiceName")) == f"com.amazonaws.eu-west-1.{service}", (
        f'{name}: ServiceName must be !Sub "com.amazonaws.${{AWS::Region}}.{service}"')
    assert spec.get("PrivateDnsEnabled") is True, f"{name}: PrivateDnsEnabled must be true"
    assert found.value(spec.get("SecurityGroupIds")) == ["dojo-EndpointSecurityGroup"], (
        f"{name}: SecurityGroupIds must be [!Ref EndpointSecurityGroup]")
    assert sorted(found.value(spec.get("SubnetIds"))) == subnets, (
        f"{name} in {environment} must sit in {subnets}, found {found.value(spec.get('SubnetIds'))}")


@pytest.mark.parametrize("environment", ["dev", "prod"])
def test_s3_is_reached_through_a_free_gateway_endpoint(environment):
    found = stack(Environment=environment)
    spec = endpoint(found, "S3Endpoint")
    assert spec.get("VpcEndpointType", "Gateway") == "Gateway", "S3Endpoint must be a Gateway endpoint"
    assert found.value(spec.get("ServiceName")) == "com.amazonaws.eu-west-1.s3"
    tables = {route_table_of(found, f"{tier}Subnet{zone}") for tier in ("App", "Data") for zone in ZONES}
    attached = {t.get("Ref") for t in spec.get("RouteTableIds", [])}
    assert attached == tables, f"S3Endpoint must serve the app and data route tables {sorted(tables)}, found {attached}"


def test_no_admin_ports_anywhere():
    found = stack()
    for group in found.resources_of("AWS::EC2::SecurityGroup"):
        for rule in ingress(found, group):
            if rule["protocol"] == "-1":
                raise AssertionError(f"{group} allows all traffic in: no rule may open everything")
            low, high = rule["ports"]
            for port in (22, 3389):
                assert not low <= port <= high, f"{group} opens port {port}: use Session Manager, no bastion"


@pytest.mark.parametrize("environment,days", [("prod", 90), ("dev", 14)])
def test_flow_logs_are_kept_for_a_bounded_time(environment, days):
    found = stack(Environment=environment)
    assert "FlowLogGroup" in found.resources_of("AWS::Logs::LogGroup"), "declare FlowLogGroup (AWS::Logs::LogGroup)"
    assert int(found.value(props(found, "FlowLogGroup").get("RetentionInDays", 0))) == days, (
        f"in {environment}, FlowLogGroup keeps logs {days} days")


def test_the_flow_log_records_all_traffic_of_the_vpc():
    found = stack()
    logs = found.resources_of("AWS::EC2::FlowLog")
    assert "VpcFlowLog" in logs, "declare VpcFlowLog (AWS::EC2::FlowLog)"
    spec = found.value(props(found, "VpcFlowLog"))
    expected = {"ResourceId": "dojo-Vpc", "ResourceType": "VPC", "TrafficType": "ALL",
                "LogDestinationType": "cloud-watch-logs", "LogGroupName": "dojo-FlowLogGroup",
                "DeliverLogsPermissionArn": ROLE, "MaxAggregationInterval": 60}
    for key, value in expected.items():
        assert str(spec.get(key)) == str(value), f"VpcFlowLog: {key} must be {value}, found {spec.get(key)}"


@pytest.fixture(scope="module")
def role():
    found = stack()
    assert "FlowLogRole" in found.resources_of("AWS::IAM::Role"), "declare FlowLogRole (AWS::IAM::Role)"
    spec = found.value(props(found, "FlowLogRole"))
    trust = spec.get("AssumeRolePolicyDocument")
    policies = [p.get("PolicyDocument") for p in spec.get("Policies", [])]
    assert not spec.get("ManagedPolicyArns"), "no managed policies: write the three actions inline"
    assert iam.validate(trust, "resource") == [], "; ".join(iam.validate(trust, "resource"))
    for policy in policies:
        assert iam.validate(policy) == [], "; ".join(iam.validate(policy))
    return trust, policies


@pytest.mark.parametrize("case,principal,account,source,expected", [
    ("flow logs of this account", FLOW_LOGS, ACCOUNT,
     f"arn:aws:ec2:eu-west-1:{ACCOUNT}:vpc-flow-log/fl-0abc", iam.ALLOW),
    ("flow logs for another account", FLOW_LOGS, "444455556666",
     "arn:aws:ec2:eu-west-1:444455556666:vpc-flow-log/fl-0abc", iam.IMPLICIT_DENY),
    ("flow logs with this account but a foreign source", FLOW_LOGS, ACCOUNT,
     "arn:aws:ec2:eu-west-1:444455556666:vpc-flow-log/fl-0abc", iam.IMPLICIT_DENY),
    ("flow logs with no source keys", FLOW_LOGS, None, None, iam.IMPLICIT_DENY),
    ("EC2", "ec2.amazonaws.com", ACCOUNT, f"arn:aws:ec2:eu-west-1:{ACCOUNT}:instance/i-0abc", iam.IMPLICIT_DENY),
])
def test_only_flow_logs_of_this_account_assume_the_role(role, case, principal, account, source, expected):
    context = {}
    if account:
        context["aws:SourceAccount"] = account
    if source:
        context["aws:SourceArn"] = source
    request = iam.Request(principal=principal, action="sts:AssumeRole", resource=ROLE, context=context)
    found = iam.evaluate(request, resource_policy=role[0])
    assert found == expected, f"{case}: expected {expected}, the trust policy gives {found}"


@pytest.mark.parametrize("case,action,resource,expected", [
    ("write a stream", "logs:PutLogEvents", f"{GROUP}:log-stream:eni-0abc-all", iam.ALLOW),
    ("create a stream", "logs:CreateLogStream", f"{GROUP}:log-stream:eni-0abc-all", iam.ALLOW),
    ("list streams", "logs:DescribeLogStreams", f"{GROUP}:*", iam.ALLOW),
    ("write another group", "logs:PutLogEvents",
     f"arn:aws:logs:eu-west-1:{ACCOUNT}:log-group:payments-audit:log-stream:x", iam.IMPLICIT_DENY),
    ("create a log group", "logs:CreateLogGroup", f"{GROUP}:*", iam.IMPLICIT_DENY),
    ("delete the log group", "logs:DeleteLogGroup", f"{GROUP}:*", iam.IMPLICIT_DENY),
    ("read the logs", "logs:GetLogEvents", f"{GROUP}:log-stream:eni-0abc-all", iam.IMPLICIT_DENY),
])
def test_the_role_only_writes_its_own_log_group(role, case, action, resource, expected):
    request = iam.Request(principal=f"arn:aws:sts::{ACCOUNT}:assumed-role/dojo-FlowLogRole/flow-logs",
                          action=action, resource=resource)
    found = iam.evaluate(request, identity=role[1])
    assert found == expected, f"{case}: expected {expected}, the role's policy gives {found}"
