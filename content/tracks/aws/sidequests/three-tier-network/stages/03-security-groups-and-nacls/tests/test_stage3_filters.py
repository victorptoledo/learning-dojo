import pytest

from network import ZONES, acl_decision, acl_of, egress, host, ingress, props, stack

GROUPS = ["AlbSecurityGroup", "AppSecurityGroup", "DataSecurityGroup"]
TCP = 6
CIDRS = ["10.40.0.0/16", "172.20.0.0/16"]


def tcp(port, **peer):
    return {"protocol": "tcp", "ports": (port, port), **peer}


@pytest.mark.parametrize("group", GROUPS)
def test_each_group_is_described_and_in_the_vpc(group):
    found = stack()
    assert group in found.resources_of("AWS::EC2::SecurityGroup"), f"declare the security group {group}"
    assert props(found, group).get("GroupDescription"), f"{group} needs a GroupDescription"
    assert found.value(props(found, group).get("VpcId")) == "dojo-Vpc", f"{group}: VpcId must be !Ref Vpc"


@pytest.mark.parametrize("group,expected", [
    ("AlbSecurityGroup", [tcp(443, cidr="0.0.0.0/0")]),
    ("AppSecurityGroup", [tcp(8080, group="AlbSecurityGroup")]),
    ("DataSecurityGroup", [tcp(5432, group="AppSecurityGroup")]),
])
def test_ingress_is_exactly_the_tier_in_front(group, expected):
    found = ingress(stack(), group)
    assert found == expected, f"{group} ingress must be {expected}, found {found}"


@pytest.mark.parametrize("group,expected", [
    ("AlbSecurityGroup", [tcp(8080, group="AppSecurityGroup")]),
    ("AppSecurityGroup", [tcp(443, cidr="0.0.0.0/0"), tcp(5432, group="DataSecurityGroup")]),
])
def test_egress_is_exactly_the_tier_behind(group, expected):
    found = egress(stack(), group)
    key = lambda rule: sorted(rule.items())  # noqa: E731
    assert sorted(found, key=key) == sorted(expected, key=key), (
        f"{group} egress must be {expected}, found {found} (no inline SecurityGroupEgress means all traffic)")


def test_the_data_tier_starts_no_connections():
    found = egress(stack(), "DataSecurityGroup")
    assert found, "DataSecurityGroup needs an inline SecurityGroupEgress, or AWS allows all traffic out"
    for rule in found:
        assert rule.get("cidr") == "127.0.0.1/32" and "group" not in rule, (
            f"DataSecurityGroup may only keep the do-nothing rule to 127.0.0.1/32, found {rule}")


@pytest.mark.parametrize("zone", ZONES)
def test_the_data_subnets_use_the_data_acl(zone):
    assert acl_of(stack(), f"DataSubnet{zone}") == "DataNetworkAcl", (
        f"associate DataSubnet{zone} with DataNetworkAcl")


def test_the_other_subnets_keep_the_default_acl():
    found = stack()
    for tier in ("Public", "App"):
        for zone in ZONES:
            assert acl_of(found, f"{tier}Subnet{zone}") is None, f"leave {tier}Subnet{zone} on the default ACL"


@pytest.mark.parametrize("vpc_cidr", CIDRS)
@pytest.mark.parametrize("zone", ZONES)
def test_the_app_tier_reaches_the_database_and_gets_its_replies(vpc_cidr, zone):
    found = stack(VpcCidr=vpc_cidr)
    client = host(found, f"AppSubnet{zone}")
    assert acl_decision(found, "DataNetworkAcl", False, TCP, 5432, client) == "allow", (
        f"VpcCidr={vpc_cidr}: tcp 5432 from {client} (AppSubnet{zone}) must come in")
    for port in (1024, 49152, 65535):
        assert acl_decision(found, "DataNetworkAcl", True, TCP, port, client) == "allow", (
            f"VpcCidr={vpc_cidr}: the reply to {client}:{port} must go out (ephemeral ports 1024-65535)")


@pytest.mark.parametrize("vpc_cidr", CIDRS)
@pytest.mark.parametrize("case,outbound,port,where", [
    ("ssh from the app tier", False, 22, "AppSubnetA"),
    ("postgres from a public subnet", False, 5432, "PublicSubnetB"),
    ("postgres from the internet", False, 5432, "internet"),
    ("postgres from another data subnet's neighbour", False, 5432, "DataSubnetC"),
    ("replies to a public subnet", True, 49152, "PublicSubnetA"),
    ("https to the internet", True, 443, "internet"),
    ("a well-known port in the app tier", True, 80, "AppSubnetB"),
])
def test_everything_else_is_denied(vpc_cidr, case, outbound, port, where):
    found = stack(VpcCidr=vpc_cidr)
    address = "52.95.110.1" if where == "internet" else host(found, where)
    assert acl_decision(found, "DataNetworkAcl", outbound, TCP, port, address) == "deny", (
        f"VpcCidr={vpc_cidr}: {case} must be denied by DataNetworkAcl")


def test_udp_and_all_traffic_rules_stay_out():
    found = stack()
    for name, entry in found.resources_of("AWS::EC2::NetworkAclEntry").items():
        protocol = str(entry["Properties"].get("Protocol"))
        assert protocol == "6", f"{name}: the data tier only needs TCP (Protocol: 6), found {protocol}"
        assert entry["Properties"].get("RuleAction") == "allow", f"{name}: the ACL denies by default, no deny rules"
