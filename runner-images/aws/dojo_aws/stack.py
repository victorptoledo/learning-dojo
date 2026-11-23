"""A CloudFormation template as a deployed stack would see it, without deploying it.

    from dojo_aws import template
    from dojo_aws.stack import Stack

    stack = Stack(template("network.yaml"), parameters={"Environment": "prod"})
    stack.condition("IsProd")                 # True
    stack.resources                           # only the resources whose Condition holds,
                                              # with Fn::If chosen and AWS::NoValue removed
    stack.value({"Fn::Select": [0, {"Fn::Cidr": [{"Fn::GetAtt": ["Vpc", "CidrBlock"]}, 9, 12]}]})
                                              # "10.40.0.0/20"
    stack.value(policy_document)              # every intrinsic resolved: ARNs as strings

Parameters take the template's defaults, overridden by `parameters`; a parameter with no
default must be passed. Pseudo parameters are fixed: account 111122223333, region
eu-west-1 (zones eu-west-1a, b and c), stack name `dojo`.

Resources get made-up but stable physical ids: the name property when the template sets
one (BucketName, TableName, RoleName...), otherwise `dojo-<LogicalId>` (buckets in lower
case), and the ARN formats of the real services, so a policy resolved here can be fed to
the IAM evaluator. `stack.value` resolves Ref, Fn::GetAtt, Fn::Sub, Fn::Join, Fn::Select,
Fn::Split, Fn::GetAZs, Fn::Cidr, Fn::If, Fn::FindInMap, Fn::Base64, Fn::ImportValue,
Fn::Length, Fn::ToJsonString and the condition functions. Nothing here talks to AWS.
"""

from __future__ import annotations

import ipaddress
import json
import re
from typing import Any

ACCOUNT = "111122223333"
REGION = "eu-west-1"
STACK_NAME = "dojo"
ZONES = [f"{REGION}{letter}" for letter in "abc"]

_NO_VALUE = object()

# The property that names a resource, by type, when the template sets one.
_NAME_PROPERTY = {
    "AWS::S3::Bucket": "BucketName",
    "AWS::DynamoDB::Table": "TableName",
    "AWS::IAM::Role": "RoleName",
    "AWS::IAM::ManagedPolicy": "ManagedPolicyName",
    "AWS::IAM::InstanceProfile": "InstanceProfileName",
    "AWS::Lambda::Function": "FunctionName",
    "AWS::Serverless::Function": "FunctionName",
    "AWS::SQS::Queue": "QueueName",
    "AWS::SNS::Topic": "TopicName",
    "AWS::Logs::LogGroup": "LogGroupName",
    "AWS::SecretsManager::Secret": "Name",
    "AWS::ElasticLoadBalancingV2::LoadBalancer": "Name",
    "AWS::ElasticLoadBalancingV2::TargetGroup": "Name",
    "AWS::RDS::DBInstance": "DBInstanceIdentifier",
}


class Stack:
    def __init__(self, tpl: dict, parameters: dict[str, Any] | None = None):
        self.template = tpl
        self.parameters = self._parameters(parameters or {})
        self._conditions: dict[str, bool] = {}
        self._evaluating: set[str] = set()
        self.resources = {
            name: self._choose(res)
            for name, res in (tpl.get("Resources") or {}).items()
            if "Condition" not in res or self.condition(res["Condition"])
        }
        self.outputs = {
            name: self._choose(out)
            for name, out in (tpl.get("Outputs") or {}).items()
            if "Condition" not in out or self.condition(out["Condition"])
        }

    # ---- parameters and conditions ---------------------------------------------------

    def _parameters(self, given: dict[str, Any]) -> dict[str, Any]:
        declared = self.template.get("Parameters") or {}
        unknown = sorted(set(given) - set(declared))
        assert not unknown, f"the template declares no parameter {unknown}"
        values = {}
        for name, spec in declared.items():
            if name in given:
                value = given[name]
            elif "Default" in spec:
                value = spec["Default"]
            else:
                raise AssertionError(f"the test must pass a value for parameter {name} (it has no Default)")
            kind = str(spec.get("Type", "String"))
            if (kind == "CommaDelimitedList" or kind.startswith("List<")) and isinstance(value, str):
                value = [item.strip() for item in value.split(",")] if value else []
            allowed = spec.get("AllowedValues")
            if allowed is not None and not isinstance(value, list):
                assert value in allowed, f"parameter {name}: {value!r} is not one of its AllowedValues {allowed}"
            values[name] = value
        return values

    def condition(self, name: str) -> bool:
        if name in self._conditions:
            return self._conditions[name]
        declared = self.template.get("Conditions") or {}
        assert name in declared, f"the template has no condition {name}"
        assert name not in self._evaluating, f"condition {name} refers to itself"
        self._evaluating.add(name)
        result = bool(self.value(declared[name]))
        self._evaluating.discard(name)
        self._conditions[name] = result
        return result

    # ---- the active template: Fn::If chosen, AWS::NoValue removed ------------------------

    def _choose(self, value: Any) -> Any:
        if isinstance(value, dict):
            if set(value) == {"Fn::If"}:
                name, when_true, when_false = value["Fn::If"]
                return self._choose(when_true if self.condition(name) else when_false)
            if value == {"Ref": "AWS::NoValue"}:
                return _NO_VALUE
            chosen = {key: self._choose(item) for key, item in value.items()}
            return {key: item for key, item in chosen.items() if item is not _NO_VALUE}
        if isinstance(value, list):
            return [item for item in (self._choose(item) for item in value) if item is not _NO_VALUE]
        return value

    def resources_of(self, type_: str) -> dict[str, dict]:
        return {name: res for name, res in self.resources.items() if res.get("Type") == type_}

    def props(self, name: str) -> dict:
        assert name in self.resources, f"no resource {name} in this stack"
        return self.resources[name].get("Properties", {}) or {}

    # ---- physical ids and attributes --------------------------------------------------

    def physical_id(self, name: str) -> str:
        res = self._resource(name)
        type_ = res.get("Type", "")
        props = res.get("Properties", {}) or {}
        prop = _NAME_PROPERTY.get(type_)
        named = self.value(props[prop]) if prop and prop in props else None
        default = f"{STACK_NAME}-{name}"
        match type_:
            case "AWS::S3::Bucket":
                return named or default.lower()
            case "AWS::SQS::Queue":
                return f"https://sqs.{REGION}.amazonaws.com/{ACCOUNT}/{named or default}"
            case "AWS::SNS::Topic":
                return f"arn:aws:sns:{REGION}:{ACCOUNT}:{named or default}"
            case "AWS::SecretsManager::Secret":
                return f"arn:aws:secretsmanager:{REGION}:{ACCOUNT}:secret:{named or default}-AbCdEf"
            case "AWS::IAM::ManagedPolicy":
                return f"arn:aws:iam::{ACCOUNT}:policy/{named or default}"
            case "AWS::ElasticLoadBalancingV2::LoadBalancer":
                return f"arn:aws:elasticloadbalancing:{REGION}:{ACCOUNT}:loadbalancer/app/{named or default}/50dc6c495c0c9188"
            case "AWS::ElasticLoadBalancingV2::TargetGroup":
                return f"arn:aws:elasticloadbalancing:{REGION}:{ACCOUNT}:targetgroup/{named or default}/73e2d6bc24d8a067"
            case "AWS::KMS::Key":
                return f"{name.lower()}-0000-4000-8000-000000000000"
        return named or default

    def attribute(self, name: str, attribute: str) -> Any:
        res = self._resource(name)
        type_ = res.get("Type", "")
        props = res.get("Properties", {}) or {}
        ident = self.physical_id(name)
        service = type_.split("::")[1].lower() if type_.count("::") == 2 else "unknown"
        arns = {
            "AWS::S3::Bucket": f"arn:aws:s3:::{ident}",
            "AWS::DynamoDB::Table": f"arn:aws:dynamodb:{REGION}:{ACCOUNT}:table/{ident}",
            "AWS::IAM::Role": f"arn:aws:iam::{ACCOUNT}:role/{ident}",
            "AWS::IAM::InstanceProfile": f"arn:aws:iam::{ACCOUNT}:instance-profile/{ident}",
            "AWS::Lambda::Function": f"arn:aws:lambda:{REGION}:{ACCOUNT}:function:{ident}",
            "AWS::Serverless::Function": f"arn:aws:lambda:{REGION}:{ACCOUNT}:function:{ident}",
            "AWS::SQS::Queue": f"arn:aws:sqs:{REGION}:{ACCOUNT}:{ident.rsplit('/', 1)[-1]}",
            "AWS::Logs::LogGroup": f"arn:aws:logs:{REGION}:{ACCOUNT}:log-group:{ident}:*",
            "AWS::KMS::Key": f"arn:aws:kms:{REGION}:{ACCOUNT}:key/{ident}",
            "AWS::SNS::Topic": ident,
            "AWS::SecretsManager::Secret": ident,
            "AWS::IAM::ManagedPolicy": ident,
            "AWS::ElasticLoadBalancingV2::LoadBalancer": ident,
            "AWS::ElasticLoadBalancingV2::TargetGroup": ident,
        }
        if attribute in ("Arn", "TopicArn", "LoadBalancerArn", "TargetGroupArn", "PolicyArn"):
            return arns.get(type_, f"arn:aws:{service}:{REGION}:{ACCOUNT}:{ident}")
        if attribute == "StreamArn" and type_ == "AWS::DynamoDB::Table":
            return arns[type_] + "/stream/2026-01-01T00:00:00.000"
        if type_ == "AWS::S3::Bucket" and attribute in ("RegionalDomainName", "DomainName"):
            return f"{ident}.s3.{REGION}.amazonaws.com" if attribute == "RegionalDomainName" else f"{ident}.s3.amazonaws.com"
        if attribute == "CidrBlock" and "CidrBlock" in props:
            return self.value(props["CidrBlock"])
        if type_ == "AWS::RDS::DBInstance":
            if attribute == "MasterUserSecret.SecretArn":
                return f"arn:aws:secretsmanager:{REGION}:{ACCOUNT}:secret:rds!db-{name.lower()}-AbCdEf"
            if attribute == "Endpoint.Address":
                return f"{ident.lower()}.c9akciq32.{REGION}.rds.amazonaws.com"
            if attribute == "Endpoint.Port":
                return "5432"
        if type_ == "AWS::CloudFront::Distribution" and attribute == "DomainName":
            return "d111111abcdef8.cloudfront.net"
        if type_ == "AWS::ElasticLoadBalancingV2::LoadBalancer" and attribute == "DNSName":
            return f"{ident.split('/')[-2]}-1234567890.{REGION}.elb.amazonaws.com"
        if attribute in ("Id", "GroupId", "AllocationId", "KeyId", "VpcId", "SubnetId"):
            return ident
        return f"{name}.{attribute}"

    def _resource(self, name: str) -> dict:
        if name not in (self.template.get("Resources") or {}):
            raise AssertionError(f"the template has no resource {name}")
        if name not in self.resources:
            raise AssertionError(f"{name} is used here but its Condition is false for these parameters")
        return self.resources[name]

    # ---- full resolution --------------------------------------------------------------

    def ref(self, name: str) -> Any:
        pseudo = {
            "AWS::AccountId": ACCOUNT,
            "AWS::Region": REGION,
            "AWS::StackName": STACK_NAME,
            "AWS::StackId": f"arn:aws:cloudformation:{REGION}:{ACCOUNT}:stack/{STACK_NAME}/0b6a8e60-0000-4000-8000-000000000000",
            "AWS::Partition": "aws",
            "AWS::URLSuffix": "amazonaws.com",
            "AWS::NotificationARNs": [],
        }
        if name in pseudo:
            return pseudo[name]
        if name in self.parameters:
            return self.parameters[name]
        return self.physical_id(name)

    def value(self, expr: Any) -> Any:
        """`expr` with every intrinsic function resolved to a plain value."""
        if isinstance(expr, list):
            return [item for item in (self.value(item) for item in expr) if item is not _NO_VALUE]
        if not isinstance(expr, dict):
            return expr
        if len(expr) == 1:
            (key, arg), = expr.items()
            if key == "Ref":
                return _NO_VALUE if arg == "AWS::NoValue" else self.ref(arg)
            if key == "Condition":
                return self.condition(arg)
            if key.startswith("Fn::"):
                return self._function(key, arg)
        resolved = {key: self.value(item) for key, item in expr.items()}
        return {key: item for key, item in resolved.items() if item is not _NO_VALUE}

    def _function(self, key: str, arg: Any) -> Any:
        match key:
            case "Fn::GetAtt":
                name, attribute = (arg.split(".", 1) if isinstance(arg, str) else (arg[0], self.value(arg[1])))
                return self.attribute(name, attribute)
            case "Fn::Sub":
                text, variables = (arg, {}) if isinstance(arg, str) else (arg[0], arg[1])
                variables = {name: self.value(item) for name, item in variables.items()}
                return re.sub(r"\$\{([^}]+)\}", lambda m: self._sub(m.group(1), variables), text)
            case "Fn::Join":
                delimiter, items = arg
                return delimiter.join(str(item) for item in self.value(items))
            case "Fn::Select":
                index, items = self.value(arg[0]), self.value(arg[1])
                return items[int(index)]
            case "Fn::Split":
                delimiter, text = arg
                return str(self.value(text)).split(delimiter)
            case "Fn::GetAZs":
                return list(ZONES)
            case "Fn::Cidr":
                block, count, bits = (self.value(item) for item in arg)
                network = ipaddress.ip_network(block)
                prefix = network.max_prefixlen - int(bits)
                assert prefix >= network.prefixlen, f"Fn::Cidr: {bits} bits do not fit in {block}"
                subnets = list(_take(network.subnets(new_prefix=prefix), int(count)))
                assert len(subnets) == int(count), f"Fn::Cidr: {block} has no room for {count} /{prefix} blocks"
                return [str(subnet) for subnet in subnets]
            case "Fn::If":
                name, when_true, when_false = arg
                return self.value(when_true if self.condition(name) else when_false)
            case "Fn::Equals":
                left, right = (self.value(item) for item in arg)
                return str(left) == str(right)
            case "Fn::And":
                return all(self.value(item) for item in arg)
            case "Fn::Or":
                return any(self.value(item) for item in arg)
            case "Fn::Not":
                return not self.value(arg[0])
            case "Fn::FindInMap":
                mapping, top, second = (self.value(item) for item in arg[:3])
                return self.template["Mappings"][mapping][top][second]
            case "Fn::Base64":
                return self.value(arg)
            case "Fn::ImportValue":
                return f"import:{self.value(arg)}"
            case "Fn::Length":
                return len(self.value(arg))
            case "Fn::ToJsonString":
                return json.dumps(self.value(arg), separators=(",", ":"))
        raise AssertionError(f"{key} is not supported by the dojo's template resolver")

    def _sub(self, name: str, variables: dict[str, Any]) -> str:
        if name.startswith("!"):
            return "${" + name[1:] + "}"
        if name in variables:
            return str(variables[name])
        if "." in name and not name.startswith("AWS::"):
            resource, attribute = name.split(".", 1)
            return str(self.attribute(resource, attribute))
        return str(self.ref(name))


def _take(items, count):
    for index, item in enumerate(items):
        if index >= count:
            return
        yield item


__all__ = ["ACCOUNT", "REGION", "STACK_NAME", "Stack", "ZONES"]
