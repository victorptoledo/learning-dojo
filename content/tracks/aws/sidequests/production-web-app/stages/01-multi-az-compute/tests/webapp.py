"""app.yaml as a deployment would see it, with the parameters a real deployment passes."""

import functools

from dojo_aws import ref, template
from dojo_aws.stack import ACCOUNT, Stack

CERTIFICATE = f"arn:aws:acm:eu-west-1:{ACCOUNT}:certificate/6f1c3d5e-2b7a-4c1e-9d0f-1a2b3c4d5e6f"
DEPLOYMENT = {
    "VpcId": "vpc-0a1b2c3d",
    "PublicSubnetIds": "subnet-pub-a,subnet-pub-b,subnet-pub-c",
    "PrivateSubnetIds": "subnet-app-a,subnet-app-b,subnet-app-c",
    "DataSubnetIds": "subnet-data-a,subnet-data-b,subnet-data-c",
    "CertificateArn": CERTIFICATE,
    "ArtifactBucket": "shop-artifacts",
    "CostCenter": "CC-1234",
    "AlertEmail": "finance@example.com",
}


@functools.cache
def tpl():
    return template("app.yaml")


@functools.cache
def stack(**overrides) -> Stack:
    declared = tpl().get("Parameters") or {}
    given = {name: value for name, value in {**DEPLOYMENT, **overrides}.items() if name in declared}
    return Stack(tpl(), given)


def one(type_, name):
    found = stack().resources_of(type_)
    assert name in found, f"declare {name} ({type_})"
    return found[name]


def props(type_, name):
    return one(type_, name).get("Properties", {}) or {}


def ingress(group):
    rules = props("AWS::EC2::SecurityGroup", group).get("SecurityGroupIngress", [])
    rules += [r["Properties"] for r in stack().resources_of("AWS::EC2::SecurityGroupIngress").values()
              if ref(r["Properties"].get("GroupId")) == group]
    found = []
    for rule in rules:
        entry = {"protocol": str(rule.get("IpProtocol")), "ports": (rule.get("FromPort"), rule.get("ToPort"))}
        if "SourceSecurityGroupId" in rule:
            entry["group"] = ref(rule["SourceSecurityGroupId"])
        if "CidrIp" in rule:
            entry["cidr"] = rule["CidrIp"]
        if "CidrIpv6" in rule:
            entry["cidr"] = rule["CidrIpv6"]
        found.append(entry)
    return found


def attributes(items) -> dict[str, str]:
    return {item.get("Key"): str(item.get("Value")).lower() for item in items or []}
