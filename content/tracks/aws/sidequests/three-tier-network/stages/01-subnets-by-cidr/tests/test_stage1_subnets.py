import ipaddress

import pytest

from dojo_aws import outputs
from network import SUBNETS, TIERS, ZONES, cidr, props, stack, tpl

TAGS = {"Public": "public", "App": "app", "Data": "data"}


def test_the_vpc_resolves_dns_names():
    vpc = props(stack(), "Vpc")
    assert vpc.get("EnableDnsSupport") is True, "set EnableDnsSupport: true on the VPC"
    assert vpc.get("EnableDnsHostnames") is True, "set EnableDnsHostnames: true on the VPC"


@pytest.mark.parametrize("subnet", SUBNETS)
def test_each_subnet_is_in_the_vpc_and_in_its_zone(subnet):
    found = stack()
    assert subnet in found.resources_of("AWS::EC2::Subnet"), f"declare the subnet {subnet}"
    assert found.value(props(found, subnet).get("VpcId")) == "dojo-Vpc", f"{subnet}: VpcId must be !Ref Vpc"
    zone = props(found, subnet).get("AvailabilityZone")
    expected = {"Fn::Select": [ZONES.index(subnet[-1]), {"Fn::GetAZs": ""}]}
    assert zone == expected, f'{subnet}: AvailabilityZone must be !Select [{ZONES.index(subnet[-1])}, !GetAZs ""]'


@pytest.mark.parametrize("vpc_cidr", ["10.40.0.0/16", "172.20.0.0/16"])
def test_the_blocks_are_cut_from_the_vpc_block_in_order(vpc_cidr):
    found = stack(VpcCidr=vpc_cidr)
    blocks = list(ipaddress.ip_network(vpc_cidr).subnets(new_prefix=20))
    for index, subnet in enumerate(SUBNETS):
        assert subnet in found.resources, f"declare the subnet {subnet}"
        assert cidr(found, subnet) == blocks[index], (
            f"with VpcCidr={vpc_cidr}, {subnet} must be {blocks[index]} (block {index} of "
            f"!Cidr [!GetAtt Vpc.CidrBlock, 9, 12]), found {cidr(found, subnet)}")


def test_only_the_public_subnets_give_public_addresses():
    found = stack()
    for subnet in SUBNETS:
        public = props(found, subnet).get("MapPublicIpOnLaunch", False) is True
        assert public == subnet.startswith("Public"), (
            f"{subnet}: MapPublicIpOnLaunch must be {'true' if subnet.startswith('Public') else 'absent or false'}")


@pytest.mark.parametrize("subnet", SUBNETS)
def test_each_subnet_is_tagged_with_its_tier(subnet):
    tags = {tag.get("Key"): tag.get("Value") for tag in props(stack(), subnet).get("Tags", [])}
    tier = next(t for t in TIERS if subnet.startswith(t))
    assert tags.get("tier") == TAGS[tier], f"{subnet} needs the tag tier={TAGS[tier]}, found {tags}"


@pytest.mark.parametrize("name,value", [
    ("VpcId", "dojo-Vpc"),
    ("PublicSubnetIds", "dojo-PublicSubnetA,dojo-PublicSubnetB,dojo-PublicSubnetC"),
    ("AppSubnetIds", "dojo-AppSubnetA,dojo-AppSubnetB,dojo-AppSubnetC"),
    ("DataSubnetIds", "dojo-DataSubnetA,dojo-DataSubnetB,dojo-DataSubnetC"),
])
def test_outputs_are_exported_for_other_stacks(name, value):
    found = outputs(tpl())
    assert name in found, f"add the output {name}"
    assert stack().value(found[name].get("Value")) == value, f"{name} must resolve to {value}"
    export = found[name].get("Export", {}).get("Name")
    assert stack().value(export) == f"dojo-{name}", f'{name}: Export Name must be !Sub "${{AWS::StackName}}-{name}"'
