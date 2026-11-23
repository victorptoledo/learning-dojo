"""Reading network.yaml as a deployed stack: subnets, routes, security groups and ACLs."""

import functools
import ipaddress

from dojo_aws import ref, template
from dojo_aws.stack import Stack

TIERS = ("Public", "App", "Data")
ZONES = "ABC"
SUBNETS = [f"{tier}Subnet{zone}" for tier in TIERS for zone in ZONES]


@functools.cache
def tpl():
    return template("network.yaml")


@functools.cache
def stack(**parameters) -> Stack:
    return Stack(tpl(), parameters)


def props(stack_, name):
    return stack_.props(name)


def cidr(stack_, subnet) -> ipaddress.IPv4Network:
    assert subnet in stack_.resources, f"declare the subnet {subnet}"
    return ipaddress.ip_network(stack_.value(props(stack_, subnet).get("CidrBlock")))


def host(stack_, subnet, number=10) -> str:
    """An address inside the subnet."""
    return str(cidr(stack_, subnet)[number])


# ---- routing -------------------------------------------------------------------------

def route_table_of(stack_, subnet) -> str:
    tables = [ref(r["Properties"].get("RouteTableId"))
              for r in stack_.resources_of("AWS::EC2::SubnetRouteTableAssociation").values()
              if ref(r["Properties"].get("SubnetId")) == subnet]
    assert len(tables) == 1, f"{subnet} must be associated with exactly one route table, found {tables}"
    return tables[0]


def routes(stack_, table) -> list[dict]:
    return [r["Properties"] for r in stack_.resources_of("AWS::EC2::Route").values()
            if ref(r["Properties"].get("RouteTableId")) == table]


def default_route(stack_, table, family="0.0.0.0/0") -> dict | None:
    key = "DestinationCidrBlock" if family == "0.0.0.0/0" else "DestinationIpv6CidrBlock"
    found = [r for r in routes(stack_, table) if r.get(key) == family]
    assert len(found) <= 1, f"{table} has several {family} routes"
    return found[0] if found else None


def zone_of(stack_, subnet) -> str:
    return stack_.value(props(stack_, subnet).get("AvailabilityZone"))


# ---- security groups -----------------------------------------------------------------

def ingress(stack_, group) -> list[dict]:
    """Inline and standalone ingress rules of a group, as plain dicts."""
    assert group in stack_.resources_of("AWS::EC2::SecurityGroup"), f"declare the security group {group}"
    rules = list(props(stack_, group).get("SecurityGroupIngress", []))
    rules += [r["Properties"] for r in stack_.resources_of("AWS::EC2::SecurityGroupIngress").values()
              if ref(r["Properties"].get("GroupId")) == group
              or stack_.value(r["Properties"].get("GroupId")) == stack_.physical_id(group)]
    return [_rule(rule, "SourceSecurityGroupId") for rule in rules]


def egress(stack_, group) -> list[dict]:
    """Effective egress: AWS adds 'all traffic to 0.0.0.0/0' unless the group lists inline rules."""
    assert group in stack_.resources_of("AWS::EC2::SecurityGroup"), f"declare the security group {group}"
    inline = props(stack_, group).get("SecurityGroupEgress")
    rules = list(inline) if inline else [{"IpProtocol": "-1", "CidrIp": "0.0.0.0/0"}]
    rules += [r["Properties"] for r in stack_.resources_of("AWS::EC2::SecurityGroupEgress").values()
              if ref(r["Properties"].get("GroupId")) == group]
    return [_rule(rule, "DestinationSecurityGroupId") for rule in rules]


def _rule(rule, peer_key) -> dict:
    found = {"protocol": str(rule.get("IpProtocol"))}
    if found["protocol"] != "-1":
        found["ports"] = (int(rule.get("FromPort", -1)), int(rule.get("ToPort", -1)))
    if peer_key in rule:
        found["group"] = ref(rule[peer_key]) or str(rule[peer_key])
    for key in ("CidrIp", "CidrIpv6", "SourcePrefixListId", "DestinationPrefixListId"):
        if key in rule:
            found["cidr"] = rule[key]
    return found


# ---- network ACLs --------------------------------------------------------------------

def acl_of(stack_, subnet) -> str | None:
    found = [ref(a["Properties"].get("NetworkAclId"))
             for a in stack_.resources_of("AWS::EC2::SubnetNetworkAclAssociation").values()
             if ref(a["Properties"].get("SubnetId")) == subnet]
    assert len(found) <= 1, f"{subnet} is associated with several network ACLs"
    return found[0] if found else None


def acl_decision(stack_, acl, outbound, protocol, port, address) -> str:
    """allow or deny, as the ACL decides: rules in RuleNumber order, first match wins."""
    entries = [stack_.value(e["Properties"]) for e in stack_.resources_of("AWS::EC2::NetworkAclEntry").values()
               if ref(e["Properties"].get("NetworkAclId")) == acl]
    entries = [e for e in entries if str(e.get("Egress", False)).lower() == str(outbound).lower()]
    for entry in sorted(entries, key=lambda e: int(e["RuleNumber"])):
        if str(entry.get("Protocol")) not in ("-1", str(protocol)):
            continue
        if "CidrBlock" not in entry or ipaddress.ip_address(address) not in ipaddress.ip_network(entry["CidrBlock"]):
            continue
        if str(entry.get("Protocol")) != "-1":
            span = entry.get("PortRange") or {}
            if not int(span.get("From", 0)) <= port <= int(span.get("To", 65535)):
                continue
        return entry["RuleAction"]
    return "deny"
