"""Audit of HTTP response security headers, and a strict header set to send instead."""

import re

ONE_YEAR = 31_536_000
MAX_AGE = re.compile(r"(?:^|;)\s*max-age\s*=\s*(\d+)\s*(?:;|$)", re.IGNORECASE)
NONCE = re.compile(r"[A-Za-z0-9+/_-]{16,}={0,2}")
WILDCARDS = {"*", "http:", "https:", "data:"}
SAMESITE = {"lax", "strict", "none"}
WEAK_REFERRERS = {"unsafe-url", "no-referrer-when-downgrade"}


def _max_age(value: str) -> int | None:
    match = MAX_AGE.search(value)
    return int(match.group(1)) if match else None


def _csp(value: str) -> dict[str, list[str]]:
    policy: dict[str, list[str]] = {}
    for directive in value.split(";"):
        parts = directive.split()
        if parts:
            policy.setdefault(parts[0].lower(), [source.lower() for source in parts[1:]])
    return policy


def _csp_findings(value: str) -> list[str]:
    policy = _csp(value)
    findings = []
    scripts = policy.get("script-src", policy.get("default-src"))
    if scripts is not None and "'unsafe-inline'" in scripts:
        findings.append("csp-unsafe-inline")
    if scripts is None or WILDCARDS & set(scripts):
        findings.append("csp-wildcard")
    if policy.get("object-src", policy.get("default-src")) != ["'none'"]:
        findings.append("csp-object-src")
    return findings


def _cookie_findings(cookie: str) -> list[str]:
    first, _, rest = cookie.partition(";")
    name = first.partition("=")[0].strip()
    attributes: dict[str, str] = {}
    for attribute in rest.split(";"):
        key, _, value = attribute.partition("=")
        if key.strip():
            attributes.setdefault(key.strip().lower(), value.strip().lower())
    findings = []
    if "secure" not in attributes:
        findings.append(f"cookie-not-secure:{name}")
    if "httponly" not in attributes:
        findings.append(f"cookie-not-httponly:{name}")
    if attributes.get("samesite") not in SAMESITE:
        findings.append(f"cookie-no-samesite:{name}")
    return findings


def check_headers(headers: list[tuple[str, str]]) -> list[str]:
    """Finding codes for these response headers, in the README's order."""
    first: dict[str, str] = {}
    cookies: list[str] = []
    powered_by = False
    for name, value in headers:
        key = name.strip().lower()
        if key == "set-cookie":
            cookies.append(value.strip())
        else:
            first.setdefault(key, value.strip())
        powered_by = powered_by or key == "x-powered-by"

    findings = []
    hsts = first.get("strict-transport-security")
    if hsts is None:
        findings.append("hsts-missing")
    elif (_max_age(hsts) or 0) < ONE_YEAR:
        findings.append("hsts-short")

    csp = first.get("content-security-policy")
    if csp is None:
        findings.append("csp-missing")
    else:
        findings += _csp_findings(csp)

    framed = csp is not None and "frame-ancestors" in _csp(csp)
    if not framed and first.get("x-frame-options", "").upper() not in {"DENY", "SAMEORIGIN"}:
        findings.append("frame-missing")

    if first.get("x-content-type-options", "").lower() != "nosniff":
        findings.append("nosniff-missing")

    referrer = first.get("referrer-policy")
    if referrer is None:
        findings.append("referrer-policy-missing")
    elif referrer.split(",")[-1].strip().lower() in WEAK_REFERRERS:
        findings.append("referrer-policy-weak")

    if any(char.isdigit() for char in first.get("server", "")) or powered_by:
        findings.append("version-leak")

    for cookie in cookies:
        findings += _cookie_findings(cookie)
    return findings


def secure_headers(nonce: str) -> list[tuple[str, str]]:
    """The recommended header set for a page, with this CSP nonce."""
    if not isinstance(nonce, str) or NONCE.fullmatch(nonce) is None:
        raise ValueError("the nonce must be 16+ characters of base64")
    csp = (
        f"default-src 'self'; script-src 'nonce-{nonce}' 'strict-dynamic'; "
        "object-src 'none'; base-uri 'none'; frame-ancestors 'none'"
    )
    return [
        ("Strict-Transport-Security", "max-age=63072000; includeSubDomains"),
        ("Content-Security-Policy", csp),
        ("X-Content-Type-Options", "nosniff"),
        ("Referrer-Policy", "strict-origin-when-cross-origin"),
        ("Permissions-Policy", "camera=(), microphone=(), geolocation=()"),
    ]
