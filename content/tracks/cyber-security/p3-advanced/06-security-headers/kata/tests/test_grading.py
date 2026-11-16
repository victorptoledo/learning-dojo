import pytest

from headers import check_headers, secure_headers

NONCE = "q8Zt3kP0vW9xYb2Lr5Nc"
STRICT_CSP = "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'"
BASE = [
    ("Strict-Transport-Security", "max-age=31536000"),
    ("Content-Security-Policy", STRICT_CSP),
    ("X-Content-Type-Options", "nosniff"),
    ("Referrer-Policy", "no-referrer"),
]


def without(name, headers=BASE):
    return [(key, value) for key, value in headers if key.lower() != name.lower()]


def with_header(name, value, headers=BASE):
    return [*without(name, headers), (name, value)]


def test_a_good_set_has_no_findings_and_names_ignore_case():
    assert check_headers(BASE) == []
    lowered = [(name.lower(), f"  {value}  ") for name, value in BASE]
    assert check_headers(lowered) == []
    upper = [(name.upper(), value) for name, value in BASE]
    assert check_headers(upper) == []


def test_hsts():
    assert check_headers(without("Strict-Transport-Security")) == ["hsts-missing"]
    for value in ["max-age=86400", "includeSubDomains", "max-age=abc", "max-age=", "max-age=31535999"]:
        assert check_headers(with_header("Strict-Transport-Security", value)) == ["hsts-short"], value
    for value in ["MAX-AGE=63072000; includeSubDomains", "includeSubDomains; max-age = 31536000 ; preload"]:
        assert check_headers(with_header("Strict-Transport-Security", value)) == [], value


def test_only_the_first_header_counts():
    weak_first = [("Strict-Transport-Security", "max-age=60"), *BASE]
    assert check_headers(weak_first) == ["hsts-short"]
    assert check_headers([*BASE, ("Content-Security-Policy", "script-src *")]) == []


def test_csp_missing_skips_the_other_csp_checks():
    headers = with_header("X-Frame-Options", "DENY", without("Content-Security-Policy"))
    assert check_headers(headers) == ["csp-missing"]
    assert check_headers(without("Content-Security-Policy")) == ["csp-missing", "frame-missing"]


def test_csp_script_sources():
    def csp(value):
        return check_headers(with_header("Content-Security-Policy", value + "; frame-ancestors 'self'"))

    assert csp("default-src 'self' 'UNSAFE-INLINE'; object-src 'none'") == ["csp-unsafe-inline"]
    assert csp("script-src 'self' 'unsafe-inline' https:; object-src 'none'") == [
        "csp-unsafe-inline", "csp-wildcard",
    ]
    for source in ["*", "http:", "HTTPS:", "data:"]:
        assert csp(f"default-src 'none'; script-src 'self' {source}; object-src 'none'") == [
            "csp-wildcard"
        ], source
    assert csp("object-src 'none'; img-src *") == ["csp-wildcard"]
    assert csp("default-src *; script-src 'self'; object-src 'none'") == []
    assert csp("SCRIPT-SRC 'self'; script-src *; object-src 'none'") == []


def test_csp_object_sources():
    def csp(value):
        return check_headers(with_header("Content-Security-Policy", value + "; frame-ancestors 'none'"))

    assert csp("default-src 'none'") == []
    assert csp("default-src 'self'") == ["csp-object-src"]
    assert csp("script-src 'self'") == ["csp-object-src"]
    assert csp("default-src 'self'; object-src 'none' 'self'") == ["csp-object-src"]
    assert csp("default-src 'self'; object-src 'NONE'") == []


def test_frame_protection():
    no_frame_csp = with_header("Content-Security-Policy", "default-src 'none'")
    assert check_headers(no_frame_csp) == ["frame-missing"]
    assert check_headers(with_header("X-Frame-Options", "sameorigin", no_frame_csp)) == []
    assert check_headers(with_header("X-Frame-Options", "DENY", no_frame_csp)) == []
    assert check_headers(with_header("X-Frame-Options", "ALLOW-FROM https://x.test", no_frame_csp)) == [
        "frame-missing"
    ]


def test_nosniff_and_referrer_policy():
    assert check_headers(with_header("X-Content-Type-Options", "NoSniff")) == []
    assert check_headers(with_header("X-Content-Type-Options", "sniff")) == ["nosniff-missing"]
    assert check_headers(without("Referrer-Policy")) == ["referrer-policy-missing"]
    assert check_headers(with_header("Referrer-Policy", "unsafe-url")) == ["referrer-policy-weak"]
    assert check_headers(with_header("Referrer-Policy", "no-referrer, No-Referrer-When-Downgrade")) == [
        "referrer-policy-weak"
    ]
    assert check_headers(with_header("Referrer-Policy", "unsafe-url, strict-origin")) == []


def test_version_leaks():
    assert check_headers([*BASE, ("Server", "nginx")]) == []
    assert check_headers([*BASE, ("Server", "nginx/1.18.0 (Ubuntu)")]) == ["version-leak"]
    assert check_headers([*BASE, ("x-powered-by", "Express")]) == ["version-leak"]


def test_every_cookie_is_checked_in_order():
    headers = [
        *BASE,
        ("Set-Cookie", "__Host-session=abc; Path=/; Secure; HttpOnly; SameSite=Lax"),
        ("set-cookie", "theme=dark; Path=/"),
        ("Set-Cookie", "csrf=xyz; secure; samesite=STRICT"),
        ("Set-Cookie", " track = 1 ; SameSite=None; httponly"),
        ("Set-Cookie", "odd=1; SameSite=Sometimes; Secure; HttpOnly"),
        ("Set-Cookie", "ok=1; Secure; HttpOnly; SameSite=None"),
    ]
    assert check_headers(headers) == [
        "cookie-not-secure:theme",
        "cookie-not-httponly:theme",
        "cookie-no-samesite:theme",
        "cookie-not-httponly:csrf",
        "cookie-not-secure:track",
        "cookie-no-samesite:odd",
    ]


def test_secure_headers():
    assert secure_headers(NONCE) == [
        ("Strict-Transport-Security", "max-age=63072000; includeSubDomains"),
        (
            "Content-Security-Policy",
            f"default-src 'self'; script-src 'nonce-{NONCE}' 'strict-dynamic'; "
            "object-src 'none'; base-uri 'none'; frame-ancestors 'none'",
        ),
        ("X-Content-Type-Options", "nosniff"),
        ("Referrer-Policy", "strict-origin-when-cross-origin"),
        ("Permissions-Policy", "camera=(), microphone=(), geolocation=()"),
    ]
    assert check_headers(secure_headers("abc+/def-_ghi12345==")) == []
    for bad in ["short", "", "a' 'unsafe-inline", "x" * 20 + "\r\nSet-Cookie: a=b", "x" * 20 + "===", None]:
        with pytest.raises(ValueError):
            secure_headers(bad)
