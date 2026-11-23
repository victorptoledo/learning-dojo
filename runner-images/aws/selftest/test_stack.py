"""The template resolver's own tests: conditions, intrinsics, physical ids and ARNs."""

import pytest

from dojo_aws import iam
from dojo_aws.stack import Stack

TEMPLATE = {
    "Parameters": {
        "Environment": {"Type": "String", "AllowedValues": ["dev", "prod"], "Default": "dev"},
        "VpcCidr": {"Type": "String", "Default": "10.40.0.0/16"},
        "Subnets": {"Type": "List<AWS::EC2::Subnet::Id>"},
    },
    "Conditions": {
        "IsProd": {"Fn::Equals": [{"Ref": "Environment"}, "prod"]},
        "IsDev": {"Fn::Not": [{"Condition": "IsProd"}]},
    },
    "Resources": {
        "Vpc": {"Type": "AWS::EC2::VPC", "Properties": {"CidrBlock": {"Ref": "VpcCidr"}}},
        "SubnetB": {
            "Type": "AWS::EC2::Subnet",
            "Properties": {
                "VpcId": {"Ref": "Vpc"},
                "AvailabilityZone": {"Fn::Select": [1, {"Fn::GetAZs": ""}]},
                "CidrBlock": {"Fn::Select": [1, {"Fn::Cidr": [{"Fn::GetAtt": ["Vpc", "CidrBlock"]}, 9, 12]}]},
            },
        },
        "ProdOnly": {"Type": "AWS::SNS::Topic", "Condition": "IsProd"},
        "Bucket": {"Type": "AWS::S3::Bucket", "Properties": {
            "Tags": [{"Fn::If": ["IsProd", {"Key": "tier", "Value": "prod"}, {"Ref": "AWS::NoValue"}]}]}},
        "Table": {"Type": "AWS::DynamoDB::Table", "Properties": {"TableName": "Signups"}},
        "Policy": {"Type": "AWS::S3::BucketPolicy", "Properties": {"PolicyDocument": {
            "Version": "2012-10-17",
            "Statement": [{"Effect": "Allow", "Principal": {"Service": "cloudfront.amazonaws.com"},
                           "Action": "s3:GetObject", "Resource": {"Fn::Sub": "${Bucket.Arn}/*"},
                           "Condition": {"StringEquals": {"AWS:SourceArn": {
                               "Fn::Sub": "arn:aws:cloudfront::${AWS::AccountId}:distribution/${Cdn}"}}}}],
        }}},
        "Cdn": {"Type": "AWS::CloudFront::Distribution"},
    },
}


def test_parameters_take_defaults_and_overrides_and_split_lists():
    stack = Stack(TEMPLATE, {"Subnets": "subnet-1, subnet-2"})
    assert stack.parameters["Environment"] == "dev"
    assert stack.parameters["Subnets"] == ["subnet-1", "subnet-2"]
    with pytest.raises(AssertionError, match="Subnets"):
        Stack(TEMPLATE)
    with pytest.raises(AssertionError, match="AllowedValues"):
        Stack(TEMPLATE, {"Subnets": "s", "Environment": "qa"})


def test_conditions_drop_resources_and_choose_fn_if():
    dev = Stack(TEMPLATE, {"Subnets": "s"})
    prod = Stack(TEMPLATE, {"Subnets": "s", "Environment": "prod"})
    assert "ProdOnly" not in dev.resources and "ProdOnly" in prod.resources
    assert dev.condition("IsDev") and not prod.condition("IsDev")
    assert dev.props("Bucket")["Tags"] == []
    assert prod.props("Bucket")["Tags"] == [{"Key": "tier", "Value": "prod"}]


def test_cidr_select_and_azs_resolve_from_the_vpc_block():
    stack = Stack(TEMPLATE, {"Subnets": "s", "VpcCidr": "172.20.0.0/16"})
    props = stack.props("SubnetB")
    assert stack.value(props["CidrBlock"]) == "172.20.16.0/20"
    assert stack.value(props["AvailabilityZone"]) == "eu-west-1b"
    assert stack.value(props["VpcId"]) == "dojo-Vpc"


def test_names_and_arns_follow_the_services_formats():
    stack = Stack(TEMPLATE, {"Subnets": "s"})
    assert stack.value({"Ref": "Bucket"}) == "dojo-bucket"
    assert stack.value({"Fn::GetAtt": ["Table", "Arn"]}) == "arn:aws:dynamodb:eu-west-1:111122223333:table/Signups"
    with pytest.raises(AssertionError, match="Condition is false"):
        stack.value({"Ref": "ProdOnly"})


def test_a_resolved_policy_goes_straight_into_the_iam_evaluator():
    stack = Stack(TEMPLATE, {"Subnets": "s"})
    policy = stack.value(stack.props("Policy")["PolicyDocument"])
    request = iam.Request(principal="cloudfront.amazonaws.com", action="s3:GetObject",
                          resource="arn:aws:s3:::dojo-bucket/index.html",
                          context={"aws:SourceArn": "arn:aws:cloudfront::111122223333:distribution/dojo-Cdn"})
    assert iam.evaluate(request, resource_policy=policy) == iam.ALLOW
    request.context["aws:SourceArn"] = "arn:aws:cloudfront::111122223333:distribution/other"
    assert iam.evaluate(request, resource_policy=policy) == iam.IMPLICIT_DENY
