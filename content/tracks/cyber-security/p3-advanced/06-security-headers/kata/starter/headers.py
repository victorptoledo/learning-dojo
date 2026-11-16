"""Audit of HTTP response security headers. Today it only checks that a few names exist."""


def check_headers(headers: list[tuple[str, str]]) -> list[str]:
    """Finding codes for these response headers, in the README's order."""
    present = dict(headers)
    findings = []
    if "Strict-Transport-Security" not in present:
        findings.append("hsts-missing")
    if "Content-Security-Policy" not in present:
        findings.append("csp-missing")
    if "X-Content-Type-Options" not in present:
        findings.append("nosniff-missing")
    return findings


def secure_headers(nonce: str) -> list[tuple[str, str]]:
    """The recommended header set for a page, with this CSP nonce."""
    return [("Content-Security-Policy", f"script-src 'nonce-{nonce}'")]
