from dojo_aws import resources, template


def test_declares_two_public_and_two_private_subnets():
    subnets = sorted(resources(template("network.yaml"), "AWS::EC2::Subnet"))
    assert subnets == ["PrivateSubnetA", "PrivateSubnetB", "PublicSubnetA", "PublicSubnetB"]
