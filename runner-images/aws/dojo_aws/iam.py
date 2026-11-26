"""An offline IAM policy evaluator for the dojo's katas.

It follows the evaluation logic AWS documents ("Policy evaluation logic" in the IAM User
Guide) for the policy types the katas use:

1. An explicit Deny in any applicable policy wins.
2. Service control policies (SCPs) and resource control policies (RCPs), when given, must
   each allow the request (an implicit deny there is final). Each list item is one level
   of the organization (a policy, or the list of policies attached there); every level
   must allow.
3. A permissions boundary, when given, must allow the request.
4. Session policies, when given, must allow the request.
5. Within one account, an Allow in an identity-based policy is enough, and so is an Allow
   in the resource-based policy that names the caller (its user or role ARN, a service,
   or "*"). A resource policy that names only the account (`arn:aws:iam::<account>:root`
   or the bare account id) delegates to that account: the caller's identity-based
   policies must allow too ("AWS account principals" in the Principal element reference;
   the KMS default key policy works the same way). Across accounts, both are needed: the
   identity policy in the caller's account and the resource policy in the resource's
   account.
6. Assuming a role (sts:AssumeRole*) always needs an Allow in the role's trust policy,
   passed as `resource_policy`; across accounts the caller's identity policy must allow too.
7. A KMS action with a key policy given as `resource_policy` always needs an Allow in the
   key policy: an identity policy works only where the key policy names the account.

It is a teaching model, not the real thing. What it does not model: IAM Access Analyzer
checks, service-specific authorization (S3 ACLs, KMS grants, VPC endpoint policies), the
special cases where a resource policy naming an IAM user or a role session bypasses a
boundary, and global condition keys AWS fills in by itself (pass the keys the request
should carry in `context`).

    from dojo_aws import iam
    policy = iam.load("policy.json")
    request = iam.Request(principal="arn:aws:iam::111122223333:role/app",
                          action="s3:GetObject", resource="arn:aws:s3:::reports/2026/q1.csv")
    assert iam.evaluate(request, identity=[policy]) == iam.ALLOW
"""

from __future__ import annotations

import ipaddress
import json
import re
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any

ALLOW = "Allow"
EXPLICIT_DENY = "ExplicitDeny"
IMPLICIT_DENY = "ImplicitDeny"

VERSION = "2012-10-17"
STATEMENT_KEYS = {"Sid", "Effect", "Principal", "NotPrincipal", "Action", "NotAction",
                  "Resource", "NotResource", "Condition"}


@dataclass
class Request:
    """One API call: who asks, for what action, on which resource, with which context keys.

    `resource_account` is the account that owns the resource; leave it out for resources
    in the caller's account (S3 ARNs carry no account, so cross-account S3 needs it).
    `context` maps condition keys to a value or a list of values, for example
    {"aws:SecureTransport": "true", "aws:PrincipalTag/team": "data"}.
    """

    principal: str
    action: str
    resource: str = "*"
    context: dict[str, Any] = field(default_factory=dict)
    resource_account: str | None = None

    @property
    def principal_account(self) -> str:
        return account_of(self.principal) or ""

    @property
    def target_account(self) -> str:
        return self.resource_account or account_of(self.resource) or self.principal_account


def account_of(arn: str) -> str | None:
    parts = arn.split(":")
    if len(parts) >= 5 and parts[0] == "arn" and re.fullmatch(r"\d{12}", parts[4]):
        return parts[4]
    if re.fullmatch(r"\d{12}", arn):
        return arn
    return None


def load(path: str | Path) -> dict:
    """A policy document from a JSON file; fails the test with the parse error."""
    text = Path(path).read_text(encoding="utf-8")
    try:
        policy = json.loads(text)
    except json.JSONDecodeError as error:
        raise AssertionError(f"{path}:{error.lineno}:{error.colno}: invalid JSON: {error.msg}") from error
    return policy


def statements(policy: dict) -> list[dict]:
    found = policy.get("Statement", [])
    return [found] if isinstance(found, dict) else list(found)


def as_list(value: Any) -> list:
    if value is None:
        return []
    return value if isinstance(value, list) else [value]


# ---- validation ----------------------------------------------------------------------

def validate(policy: Any, kind: str = "identity") -> list[str]:
    """Structural problems in a policy, as messages (empty when it is well formed).

    `kind` is "identity" (also boundaries, session policies and SCPs: no Principal) or
    "resource" (bucket policies, trust policies, queue policies: Principal required).
    """
    problems: list[str] = []
    if not isinstance(policy, dict):
        return ["a policy is a JSON object with Version and Statement"]
    unknown = set(policy) - {"Version", "Id", "Statement"}
    if unknown:
        problems.append(f"unknown top-level keys: {sorted(unknown)}")
    if policy.get("Version") != VERSION:
        problems.append(f'Version must be "{VERSION}" (policy variables need it)')
    if "Statement" not in policy:
        problems.append("the policy has no Statement")
        return problems
    sids = []
    for index, statement in enumerate(statements(policy)):
        where = f"Statement[{index}]"
        if not isinstance(statement, dict):
            problems.append(f"{where} is not an object")
            continue
        if "Sid" in statement:
            sids.append(statement["Sid"])
            if not re.fullmatch(r"[A-Za-z0-9]*", str(statement["Sid"])):
                problems.append(f"{where}: a Sid holds only letters and digits")
        unknown = set(statement) - STATEMENT_KEYS
        if unknown:
            problems.append(f"{where}: unknown keys {sorted(unknown)}")
        if statement.get("Effect") not in (ALLOW, "Deny"):
            problems.append(f'{where}: Effect must be "Allow" or "Deny"')
        if ("Action" in statement) == ("NotAction" in statement):
            problems.append(f"{where}: give exactly one of Action and NotAction")
        for action in as_list(statement.get("Action")) + as_list(statement.get("NotAction")):
            if action != "*" and not re.fullmatch(r"[a-z0-9-]+:[A-Za-z0-9*?]+", str(action)):
                problems.append(f"{where}: `{action}` is not service:Action")
        has_principal = "Principal" in statement or "NotPrincipal" in statement
        if kind == "resource" and not has_principal:
            problems.append(f"{where}: a resource-based policy needs a Principal")
        if kind != "resource" and has_principal:
            problems.append(f"{where}: an identity-based policy has no Principal")
        if ("Resource" in statement) and ("NotResource" in statement):
            problems.append(f"{where}: give only one of Resource and NotResource")
        if kind != "resource" and "Resource" not in statement and "NotResource" not in statement:
            problems.append(f"{where}: an identity-based policy needs Resource or NotResource")
        for operator in (statement.get("Condition") or {}):
            if _parse_operator(operator) is None:
                problems.append(f"{where}: unknown condition operator `{operator}`")
    duplicates = sorted({sid for sid in sids if sids.count(sid) > 1})
    if duplicates:
        problems.append(f"duplicate Sids: {duplicates}")
    return problems


# ---- matching ------------------------------------------------------------------------

_VARIABLE = re.compile(r"\$\{([^}]+)\}")


def _substitute(pattern: str, context: dict[str, Any]) -> str | None:
    """Policy variables: ${aws:username}, ${aws:PrincipalTag/team}, ${*} ${?} ${$}.

    A variable whose key is missing from the context makes the value match nothing
    (AWS treats it as an unmatched string), unless it has a default: ${key, 'default'}.
    """
    missing = False

    def replace(found: re.Match) -> str:
        nonlocal missing
        inner = found.group(1).strip()
        if inner in ("*", "?", "$"):
            return {"*": "\x00STAR\x00", "?": "\x00QMARK\x00", "$": "$"}[inner]
        key, _, default = inner.partition(",")
        value = _lookup(context, key.strip())
        if value is None:
            default = default.strip()
            if default.startswith("'") and default.endswith("'") and len(default) >= 2:
                return default[1:-1]
            missing = True
            return ""
        return str(as_list(value)[0])

    result = _VARIABLE.sub(replace, pattern)
    return None if missing else result


def _match(pattern: str, value: str, context: dict, ignore_case: bool) -> bool:
    expanded = _substitute(pattern, context)
    if expanded is None:
        return False
    parts = expanded.split("\x00")
    regex = ""
    for part in parts:
        if part == "STAR":
            regex += re.escape("*")
        elif part == "QMARK":
            regex += re.escape("?")
        else:
            regex += "".join(".*" if c == "*" else "." if c == "?" else re.escape(c) for c in part)
    flags = re.IGNORECASE | re.DOTALL if ignore_case else re.DOTALL
    return re.fullmatch(regex, value, flags) is not None


def _action_matches(statement: dict, request: Request) -> bool:
    if "Action" in statement:
        return any(_match(str(a), request.action, {}, True) for a in as_list(statement["Action"]))
    return not any(_match(str(a), request.action, {}, True) for a in as_list(statement["NotAction"]))


def _resource_matches(statement: dict, request: Request) -> bool:
    if "Resource" in statement:
        return any(_match(str(r), request.resource, request.context, False)
                   for r in as_list(statement["Resource"]))
    if "NotResource" in statement:
        return not any(_match(str(r), request.resource, request.context, False)
                       for r in as_list(statement["NotResource"]))
    return True  # resource-based policies may leave Resource out: the resource itself


def _principal_values(principal: Any) -> list[tuple[str, str]]:
    if principal == "*":
        return [("AWS", "*")]
    if isinstance(principal, dict):
        return [(kind, str(value)) for kind, values in principal.items() for value in as_list(values)]
    return [("AWS", str(principal))]


def _is_account_principal(kind: str, value: str) -> bool:
    """`"AWS": "111122223333"` or `"AWS": "arn:aws:iam::111122223333:root"`."""
    return kind == "AWS" and (re.fullmatch(r"\d{12}", value) is not None or value.endswith(":root"))


def _one_principal(kind: str, value: str, request: Request) -> bool:
    caller = request.principal
    if value == "*":
        return True
    if kind == "Service":
        return caller == value
    if kind == "Federated":
        return caller == value
    if kind != "AWS":
        return False
    if _is_account_principal(kind, value):
        # The account (or its root ARN) matches any principal of that account, but only
        # delegates: see `_grants_directly` and `evaluate`.
        account = account_of(value)
        return account is not None and request.principal_account == account
    if caller == value:
        return True
    # A role ARN also matches the sessions of that role (sts assumed-role ARNs).
    found = re.fullmatch(r"arn:aws:iam::(\d{12}):role/(?:.*/)?([^/]+)", value)
    session = re.fullmatch(r"arn:aws:sts::(\d{12}):assumed-role/([^/]+)/.+", caller)
    return bool(found and session and found.groups() == session.groups())


def _grants_directly(statement: dict, request: Request) -> bool:
    """Whether a matching resource-policy statement names the caller itself.

    True for a user or role ARN (and its sessions), a service, a federated principal,
    `"*"` or a NotPrincipal; false when the caller matches only through an account
    principal, which delegates the decision to the account's identity-based policies.
    """
    if "Principal" not in statement:
        return True
    return any(_one_principal(kind, value, request) and not _is_account_principal(kind, value)
               for kind, value in _principal_values(statement["Principal"]))


def _principal_matches(statement: dict, request: Request) -> bool:
    if "Principal" in statement:
        return any(_one_principal(k, v, request) for k, v in _principal_values(statement["Principal"]))
    if "NotPrincipal" in statement:
        return not any(_one_principal(k, v, request)
                       for k, v in _principal_values(statement["NotPrincipal"]))
    return True


# ---- conditions ----------------------------------------------------------------------

_OPERATORS = {
    "StringEquals", "StringNotEquals", "StringEqualsIgnoreCase", "StringNotEqualsIgnoreCase",
    "StringLike", "StringNotLike", "NumericEquals", "NumericNotEquals", "NumericLessThan",
    "NumericLessThanEquals", "NumericGreaterThan", "NumericGreaterThanEquals", "DateEquals",
    "DateNotEquals", "DateLessThan", "DateLessThanEquals", "DateGreaterThan",
    "DateGreaterThanEquals", "Bool", "BinaryEquals", "IpAddress", "NotIpAddress", "ArnEquals",
    "ArnLike", "ArnNotEquals", "ArnNotLike", "Null",
}
_NEGATED = {"StringNotEquals", "StringNotEqualsIgnoreCase", "StringNotLike", "NumericNotEquals",
            "DateNotEquals", "NotIpAddress", "ArnNotEquals", "ArnNotLike"}


def _parse_operator(operator: str) -> tuple[str | None, str, bool] | None:
    prefix = None
    for candidate in ("ForAnyValue:", "ForAllValues:"):
        if operator.startswith(candidate):
            prefix, operator = candidate[:-1], operator[len(candidate):]
    if_exists = operator.endswith("IfExists") and operator != "Null"
    base = operator.removesuffix("IfExists") if if_exists else operator
    if base not in _OPERATORS:
        return None
    return prefix, base, if_exists


def _lookup(context: dict[str, Any], key: str) -> Any:
    """Condition keys are case-insensitive; the part after a tag key's slash is kept as is."""
    for name, value in context.items():
        if name.lower() == key.lower():
            return value
    return None


def _date(value: Any) -> datetime:
    text = str(value)
    if re.fullmatch(r"\d+", text):
        return datetime.fromtimestamp(int(text))
    return datetime.fromisoformat(text.replace("Z", "+00:00"))


def _compare(base: str, actual: Any, expected: Any, context: dict) -> bool:
    """One context value against one policy value, for a positive operator."""
    actual_text, expected_text = str(actual), str(expected)
    if base == "StringEquals":
        expanded = _substitute(expected_text, context)
        return expanded is not None and actual_text == expanded
    if base == "StringEqualsIgnoreCase":
        expanded = _substitute(expected_text, context)
        return expanded is not None and actual_text.lower() == expanded.lower()
    if base in ("StringLike", "ArnLike"):
        return _match(expected_text, actual_text, context, False)
    if base == "ArnEquals":
        return _match(expected_text, actual_text, context, False)
    if base == "Bool":
        return actual_text.lower() == expected_text.lower()
    if base == "BinaryEquals":
        return actual_text == expected_text
    if base == "IpAddress":
        try:
            return ipaddress.ip_address(actual_text) in ipaddress.ip_network(expected_text, strict=False)
        except ValueError:
            return False
    if base.startswith("Numeric"):
        try:
            left, right = float(actual_text), float(expected_text)
        except ValueError:
            return False
        return _ordered(base.removeprefix("Numeric"), left, right)
    if base.startswith("Date"):
        try:
            left, right = _date(actual), _date(expected)
        except ValueError:
            return False
        return _ordered(base.removeprefix("Date"), left, right)
    raise ValueError(base)


def _ordered(relation: str, left: Any, right: Any) -> bool:
    return {
        "Equals": left == right,
        "LessThan": left < right,
        "LessThanEquals": left <= right,
        "GreaterThan": left > right,
        "GreaterThanEquals": left >= right,
    }[relation]


_POSITIVE = {
    "StringNotEquals": "StringEquals", "StringNotEqualsIgnoreCase": "StringEqualsIgnoreCase",
    "StringNotLike": "StringLike", "NumericNotEquals": "NumericEquals",
    "DateNotEquals": "DateEquals", "NotIpAddress": "IpAddress", "ArnNotEquals": "ArnEquals",
    "ArnNotLike": "ArnLike",
}


def _condition_holds(operator: str, key: str, expected: Any, context: dict) -> bool:
    parsed = _parse_operator(operator)
    if parsed is None:
        raise ValueError(f"unknown condition operator `{operator}`")
    prefix, base, if_exists = parsed
    actual = _lookup(context, key)
    expected_values = as_list(expected)
    if base == "Null":
        present = actual is not None and as_list(actual) != []
        return any(str(value).lower() == ("false" if present else "true") for value in expected_values)
    if actual is None or as_list(actual) == []:
        if if_exists:
            return True
        if prefix == "ForAllValues":
            return True
        return base in _NEGATED
    negated = base in _NEGATED
    positive = _POSITIVE.get(base, base)

    def one(value: Any) -> bool:
        hit = any(_compare(positive, value, item, context) for item in expected_values)
        return not hit if negated else hit

    values = as_list(actual)
    if prefix == "ForAllValues":
        return all(one(value) for value in values)
    if prefix == "ForAnyValue":
        return any(one(value) for value in values)
    return one(values[0]) if len(values) == 1 else any(one(value) for value in values)


def _conditions_hold(statement: dict, request: Request) -> bool:
    for operator, keys in (statement.get("Condition") or {}).items():
        for key, expected in keys.items():
            if not _condition_holds(operator, key, expected, request.context):
                return False
    return True


# ---- evaluation ----------------------------------------------------------------------

def applies(statement: dict, request: Request) -> bool:
    """Whether one statement matches the request (principal, action, resource, conditions)."""
    return (_principal_matches(statement, request) and _action_matches(statement, request)
            and _resource_matches(statement, request) and _conditions_hold(statement, request))


def decide(policies: list[dict], request: Request) -> str:
    """ALLOW, EXPLICIT_DENY or IMPLICIT_DENY for a set of policies of one type."""
    allowed = False
    for policy in policies:
        for statement in statements(policy):
            if not applies(statement, request):
                continue
            if statement.get("Effect") == "Deny":
                return EXPLICIT_DENY
            allowed = True
    return ALLOW if allowed else IMPLICIT_DENY


def evaluate(request: Request, identity: list[dict] | None = None,
             resource_policy: dict | None = None, boundary: dict | None = None,
             scps: list | None = None, rcps: list | None = None,
             session: list[dict] | None = None) -> str:
    """The final decision for one request: ALLOW, EXPLICIT_DENY or IMPLICIT_DENY.

    `identity` holds the caller's identity-based policies (managed and inline),
    `resource_policy` the policy on the resource (bucket policy, trust policy, key policy,
    queue policy), `boundary` the caller's permissions boundary, `scps` and `rcps` one
    item per organization level (root, each OU, the account), where an item is a policy
    or a list of the policies attached at that level (for example FullAWSAccess plus a
    guardrail), and `session` the session policies passed to AssumeRole.
    A `None` policy type does not apply; an empty list for scps or rcps means "the
    organization attaches nothing", which denies everything, as in AWS.
    """
    identity = identity or []
    if not request.principal.startswith("arn:"):
        # A service principal (lambda.amazonaws.com) or a federated one has no identity
        # policies of its own: only resource-based policies can let it in.
        identity = []
    resource_policies = [resource_policy] if resource_policy else []
    layers: list[list[dict]] = []
    for levels in (scps, rcps):
        if levels is not None:
            layers.extend([level if isinstance(level, list) else [level] for level in levels] or [[]])
    every = identity + resource_policies + ([boundary] if boundary else []) + (session or [])
    every += [policy for layer in layers for policy in layer]
    if decide(every, request) == EXPLICIT_DENY:
        return EXPLICIT_DENY
    for layer in layers:
        if decide(layer, request) != ALLOW:
            return IMPLICIT_DENY
    if boundary is not None and decide([boundary], request) != ALLOW:
        return IMPLICIT_DENY
    if session is not None and decide(session, request) != ALLOW:
        return IMPLICIT_DENY
    by_identity = decide(identity, request) == ALLOW
    granting = [statement for policy in resource_policies for statement in statements(policy)
                if statement.get("Effect") == ALLOW and applies(statement, request)]
    by_resource = bool(granting)
    if request.action.lower().startswith("sts:assumerole") and resource_policies:
        # A role's trust policy must always allow; an identity policy alone never lets
        # anyone assume a role.
        if not by_resource:
            return IMPLICIT_DENY
    if request.action.lower().startswith("kms:") and resource_policies and not by_resource:
        # A KMS key policy must always allow, directly or through the account principal
        # that enables IAM policies: "No AWS principal, including the account root user or
        # key creator, has any permissions to a KMS key unless they are explicitly allowed"
        # by the key policy (https://docs.aws.amazon.com/kms/latest/developerguide/key-policies.html).
        return IMPLICIT_DENY
    if request.principal_account and request.target_account != request.principal_account:
        return ALLOW if by_identity and by_resource else IMPLICIT_DENY
    # Same account. A resource policy that names the caller (its user or role ARN, a
    # service, "*") is enough by itself. One that only names the account delegates: the
    # caller's identity-based policies must allow too, as in "AWS account principals"
    # (https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_principal.html#principal-accounts)
    # and in KMS key policies that "allow access to the AWS account and enable IAM policies"
    # (https://docs.aws.amazon.com/kms/latest/developerguide/key-policy-default.html#key-policy-default-allow-root-enable-iam).
    # Evaluation order: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_evaluation-logic.html
    by_resource_directly = any(_grants_directly(statement, request) for statement in granting)
    return ALLOW if by_identity or by_resource_directly else IMPLICIT_DENY


def allowed(request: Request, **policies: Any) -> bool:
    return evaluate(request, **policies) == ALLOW


__all__ = ["ALLOW", "EXPLICIT_DENY", "IMPLICIT_DENY", "Request", "account_of", "allowed",
           "applies", "decide", "evaluate", "load", "statements", "validate"]
