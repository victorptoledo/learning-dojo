import pytest

from dojo_aws import ref
from network import ZONES, default_route, props, route_table_of, routes, stack, tpl, zone_of

ENVIRONMENTS = ["dev", "prod"]


def test_the_environment_parameter_chooses_dev_or_prod():
    declared = tpl().get("Parameters", {}).get("Environment", {})
    assert declared.get("AllowedValues") == ["dev", "prod"], "Environment needs AllowedValues: [dev, prod]"
    assert declared.get("Default") == "dev", "Environment must default to dev"
    assert stack(Environment="prod").condition("IsProd") is True, "IsProd must be true when Environment is prod"
    assert stack(Environment="dev").condition("IsProd") is False, "IsProd must be false when Environment is dev"


@pytest.mark.parametrize("environment", ENVIRONMENTS)
def test_every_reference_points_at_a_resource_that_exists(environment):
    found = stack(Environment=environment)
    for name, res in found.resources.items():
        found.value(res.get("Properties", {}))
    for name, out in found.outputs.items():
        found.value(out.get("Value"))


def test_the_internet_gateway_is_attached_to_the_vpc():
    found = stack()
    gateways = found.resources_of("AWS::EC2::InternetGateway")
    assert len(gateways) == 1, f"declare one internet gateway, found {sorted(gateways)}"
    attachment = found.resources_of("AWS::EC2::VPCGatewayAttachment").get("GatewayAttachment")
    assert attachment, "attach it with a VPCGatewayAttachment called GatewayAttachment"
    assert ref(attachment["Properties"].get("VpcId")) == "Vpc"
    assert ref(attachment["Properties"].get("InternetGatewayId")) in gateways


@pytest.mark.parametrize("environment", ENVIRONMENTS)
@pytest.mark.parametrize("zone", ZONES)
def test_public_subnets_go_out_through_the_internet_gateway(environment, zone):
    found = stack(Environment=environment)
    table = route_table_of(found, f"PublicSubnet{zone}")
    route = default_route(found, table)
    assert route and ref(route.get("GatewayId")) in found.resources_of("AWS::EC2::InternetGateway"), (
        f"PublicSubnet{zone}'s route table needs 0.0.0.0/0 -> the internet gateway")
    name = next(n for n, r in found.resources_of("AWS::EC2::Route").items() if r["Properties"] is route)
    depends = found.resources[name].get("DependsOn", [])
    depends = [depends] if isinstance(depends, str) else depends
    assert "GatewayAttachment" in depends, f"{name} needs DependsOn: GatewayAttachment"


@pytest.mark.parametrize("environment,count", [("dev", 1), ("prod", 3)])
def test_one_nat_gateway_in_dev_and_one_per_zone_in_prod(environment, count):
    found = stack(Environment=environment)
    nats = found.resources_of("AWS::EC2::NatGateway")
    assert len(nats) == count, f"{environment} must run {count} NAT gateway(s), found {sorted(nats)}"
    zones = set()
    for name, nat in nats.items():
        subnet = ref(nat["Properties"].get("SubnetId"))
        assert subnet and subnet.startswith("PublicSubnet"), f"{name} must sit in a public subnet"
        zones.add(zone_of(found, subnet))
        allocation = found.value(nat["Properties"].get("AllocationId"))
        eips = {found.physical_id(e) for e in found.resources_of("AWS::EC2::EIP")}
        assert allocation in eips, f"{name}: AllocationId must be !GetAtt <its EIP>.AllocationId"
    assert len(zones) == count, f"the NAT gateways of {environment} must be in {count} different zones"
    for name, eip in found.resources_of("AWS::EC2::EIP").items():
        assert eip.get("Properties", {}).get("Domain") == "vpc", f"{name}: Domain must be vpc"
    assert len(found.resources_of("AWS::EC2::EIP")) == count, "one Elastic IP per NAT gateway, no idle ones"


@pytest.mark.parametrize("environment", ENVIRONMENTS)
@pytest.mark.parametrize("zone", ZONES)
def test_app_subnets_go_out_through_nat_of_their_zone_when_there_is_one(environment, zone):
    found = stack(Environment=environment)
    subnet = f"AppSubnet{zone}"
    route = default_route(found, route_table_of(found, subnet))
    assert route is not None, f"{subnet}'s route table needs a 0.0.0.0/0 route to a NAT gateway"
    assert "GatewayId" not in route, f"{subnet} must never route to the internet gateway"
    nat = ref(route.get("NatGatewayId"))
    assert nat in found.resources_of("AWS::EC2::NatGateway"), f"{subnet}: 0.0.0.0/0 must go to a NAT gateway"
    nat_zone = zone_of(found, ref(props(found, nat).get("SubnetId")))
    if environment == "prod":
        assert nat_zone == zone_of(found, subnet), f"in prod, {subnet} must use the NAT gateway of its own zone"
    else:
        assert nat == "NatGatewayA", f"in dev, {subnet} must use NatGatewayA, the only one"


def test_each_zone_of_the_app_tier_has_its_own_route_table():
    found = stack(Environment="prod")
    tables = {route_table_of(found, f"AppSubnet{zone}") for zone in ZONES}
    public = {route_table_of(found, f"PublicSubnet{zone}") for zone in ZONES}
    assert len(tables) == 3 and not tables & public, "use one app route table per zone"


@pytest.mark.parametrize("environment", ENVIRONMENTS)
@pytest.mark.parametrize("zone", ZONES)
def test_data_subnets_have_no_way_out(environment, zone):
    found = stack(Environment=environment)
    table = route_table_of(found, f"DataSubnet{zone}")
    app_tables = {route_table_of(found, f"AppSubnet{z}") for z in ZONES}
    assert table not in app_tables, f"DataSubnet{zone} must not share a route table with the app tier"
    assert default_route(found, table) is None, f"DataSubnet{zone} must have no 0.0.0.0/0 route"
    assert default_route(found, table, "::/0") is None, f"DataSubnet{zone} must have no ::/0 route"
    for route in routes(found, table):
        assert not {"GatewayId", "NatGatewayId", "EgressOnlyInternetGatewayId"} & set(route), (
            f"DataSubnet{zone}'s route table must not reach a gateway: {route}")
