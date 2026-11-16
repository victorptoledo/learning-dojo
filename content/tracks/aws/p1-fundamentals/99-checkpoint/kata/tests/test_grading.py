import ipaddress
import json

import pytest

from dojo_aws import getatt, guard, outputs, ref, resources, template

SUBNETS = {
    "PublicSubnetA": ("10.20.0.0/24", 0),
    "PublicSubnetB": ("10.20.1.0/24", 1),
    "PrivateSubnetA": ("10.20.16.0/20", 0),
    "PrivateSubnetB": ("10.20.32.0/20", 1),
}


@pytest.fixture(scope="module")
def tpl():
    return template("network.yaml")


def props(res):
    return res.get("Properties", {})


def route_table_of(tpl, subnet):
    tables = [ref(props(a).get("RouteTableId"))
              for a in resources(tpl, "AWS::EC2::SubnetRouteTableAssociation").values()
              if ref(props(a).get("SubnetId")) == subnet]
    assert len(tables) == 1, f"{subnet} must be associated with exactly one route table, found {tables}"
    return tables[0]


def default_route(tpl, table):
    routes = [props(r) for r in resources(tpl, "AWS::EC2::Route").values()
              if ref(props(r).get("RouteTableId")) == table
              and props(r).get("DestinationCidrBlock") == "0.0.0.0/0"]
    assert len(routes) <= 1, f"{table} has several default routes"
    return routes[0] if routes else None


def az_index(tpl, subnet):
    assert subnet in resources(tpl), f"declare the subnet {subnet}"
    zone = props(resources(tpl)[subnet]).get("AvailabilityZone")
    expected = [{"Fn::Select": [i, {"Fn::GetAZs": ""}]} for i in range(2)]
    assert zone in expected, f"{subnet}: AvailabilityZone must be !Select [0 or 1, !GetAZs \"\"], found {json.dumps(zone)}"
    return expected.index(zone)


@pytest.mark.parametrize("subnet", sorted(SUBNETS))
def test_each_subnet_has_its_cidr_and_az_in_the_vpc(tpl, subnet):
    found = resources(tpl, "AWS::EC2::Subnet")
    assert subnet in found, f"declare the subnet {subnet}"
    res = found[subnet]
    cidr, zone = SUBNETS[subnet]
    assert ref(props(res).get("VpcId")) == "Vpc", f"{subnet}: VpcId must be !Ref Vpc"
    assert props(res).get("CidrBlock") == cidr, f"{subnet}: CidrBlock must be {cidr}"
    assert az_index(tpl, subnet) == zone, f"{subnet} must be in AZ index {zone}"


def test_subnets_do_not_overlap_and_fit_in_the_vpc(tpl):
    vpc = ipaddress.ip_network(props(resources(tpl)["Vpc"])["CidrBlock"])
    networks = [ipaddress.ip_network(props(r)["CidrBlock"])
                for r in resources(tpl, "AWS::EC2::Subnet").values()]
    assert all(n.subnet_of(vpc) for n in networks), "every subnet must be inside 10.20.0.0/16"
    for i, first in enumerate(networks):
        for second in networks[i + 1:]:
            assert not first.overlaps(second), f"{first} overlaps {second}"


@pytest.mark.parametrize("subnet", ["PublicSubnetA", "PublicSubnetB"])
def test_public_subnets_route_to_the_internet_gateway(tpl, subnet):
    route = default_route(tpl, route_table_of(tpl, subnet))
    assert route and ref(route.get("GatewayId")) == "InternetGateway", (
        f"{subnet}'s route table needs 0.0.0.0/0 -> !Ref InternetGateway"
    )


@pytest.mark.parametrize("subnet", ["PrivateSubnetA", "PrivateSubnetB"])
def test_private_subnets_go_out_through_the_nat_of_their_own_az(tpl, subnet):
    route = default_route(tpl, route_table_of(tpl, subnet))
    assert route is not None, f"{subnet}'s route table needs a default route to a NAT gateway"
    assert "GatewayId" not in route, f"{subnet} must not route to the internet gateway"
    nat = ref(route.get("NatGatewayId"))
    assert nat in resources(tpl, "AWS::EC2::NatGateway"), f"{subnet}: 0.0.0.0/0 must go to a NAT gateway"
    nat_subnet = ref(props(resources(tpl)[nat]).get("SubnetId"))
    assert nat_subnet in ("PublicSubnetA", "PublicSubnetB"), f"{nat} must sit in a public subnet"
    assert az_index(tpl, nat_subnet) == az_index(tpl, subnet), (
        f"{subnet} must use the NAT gateway in its own AZ, not {nat} in {nat_subnet}"
    )


def test_private_subnets_have_their_own_route_table_per_az(tpl):
    tables = {route_table_of(tpl, s) for s in ("PrivateSubnetA", "PrivateSubnetB")}
    public = {route_table_of(tpl, s) for s in ("PublicSubnetA", "PublicSubnetB")}
    assert len(tables) == 2 and not tables & public, "use one private route table per AZ"


def test_one_nat_gateway_per_az_each_with_an_elastic_ip(tpl):
    nats = resources(tpl, "AWS::EC2::NatGateway")
    assert len(nats) == 2, f"expected two NAT gateways, found {sorted(nats)}"
    zones = set()
    for name, nat in nats.items():
        allocation = getatt(props(nat).get("AllocationId"))
        assert allocation and allocation[1] == "AllocationId" and \
            resources(tpl)[allocation[0]]["Type"] == "AWS::EC2::EIP", (
                f"{name}: AllocationId must be !GetAtt <an EIP>.AllocationId")
        zones.add(az_index(tpl, ref(props(nat).get("SubnetId"))))
    assert zones == {0, 1}, "the two NAT gateways must be in different AZs"


def test_s3_gateway_endpoint_serves_the_private_route_tables(tpl):
    endpoints = [props(e) for e in resources(tpl, "AWS::EC2::VPCEndpoint").values()]
    s3 = [e for e in endpoints if e.get("ServiceName") == {"Fn::Sub": "com.amazonaws.${AWS::Region}.s3"}]
    assert len(s3) == 1, 'add one VPCEndpoint with ServiceName !Sub "com.amazonaws.${AWS::Region}.s3"'
    assert s3[0].get("VpcEndpointType", "Gateway") == "Gateway"
    tables = {ref(t) for t in s3[0].get("RouteTableIds", [])}
    private = {route_table_of(tpl, s) for s in ("PrivateSubnetA", "PrivateSubnetB")}
    assert tables == private, f"RouteTableIds must be the two private route tables, found {tables}"


def ingress(tpl, group):
    found = resources(tpl, "AWS::EC2::SecurityGroup")
    assert group in found, f"declare the security group {group}"
    return props(found[group]).get("SecurityGroupIngress", [])


@pytest.mark.parametrize("group,port,source", [
    ("AppSecurityGroup", 8080, "AlbSecurityGroup"),
    ("DatabaseSecurityGroup", 5432, "AppSecurityGroup"),
])
def test_tiers_are_chained_by_security_group_reference(tpl, group, port, source):
    rules = ingress(tpl, group)
    expected = {"IpProtocol": "tcp", "FromPort": port, "ToPort": port,
                "SourceSecurityGroupId": {"Ref": source}}
    assert rules == [expected], f"{group} must allow only tcp {port} from !Ref {source}, found {rules}"


def test_only_the_load_balancer_is_reachable_from_the_internet(tpl):
    for name, group in resources(tpl, "AWS::EC2::SecurityGroup").items():
        for rule in props(group).get("SecurityGroupIngress", []):
            if rule.get("CidrIp") == "0.0.0.0/0" or rule.get("CidrIpv6") == "::/0":
                assert name == "AlbSecurityGroup", f"{name} must not be open to the internet"


def test_passes_the_network_guard_rules():
    failures = guard("tests/network.guard", "network.yaml")
    assert failures == [], "; ".join(failures)


def test_outputs_the_vpc_and_subnet_ids(tpl):
    found = outputs(tpl)
    assert ref(found.get("VpcId", {}).get("Value")) == "Vpc"
    for name, subnets in [("PublicSubnetIds", ["PublicSubnetA", "PublicSubnetB"]),
                          ("PrivateSubnetIds", ["PrivateSubnetA", "PrivateSubnetB"])]:
        value = found.get(name, {}).get("Value")
        assert value == {"Fn::Join": [",", [{"Ref": s} for s in subnets]]}, (
            f'{name} must be !Join [",", [!Ref {subnets[0]}, !Ref {subnets[1]}]]'
        )
