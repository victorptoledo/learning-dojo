# Security Headers Audit

Write two functions in `headers.py`. Headers arrive as a **list of `(name, value)` pairs**,
because `Set-Cookie` can appear many times. Header names are case-insensitive and values are
stripped of surrounding spaces. For every header except `Set-Cookie`, only the **first**
occurrence counts.

## `check_headers(headers) -> list[str]`

Returns the finding codes, in this order (an empty list means no findings):

| Code | When |
| --- | --- |
| `hsts-missing` | no `Strict-Transport-Security` |
| `hsts-short` | HSTS present but without a `max-age=<digits>` directive (names case-insensitive, `;`-separated), or with a value below `31536000` (one year) |
| `csp-missing` | no `Content-Security-Policy` (the checks below are then skipped) |
| `csp-unsafe-inline` | the **script sources** contain `'unsafe-inline'` |
| `csp-wildcard` | the script sources contain `*`, `http:`, `https:` or `data:`, or there are no script sources at all |
| `csp-object-src` | the **object sources** are not exactly `'none'` |
| `frame-missing` | the CSP has no `frame-ancestors` directive and `X-Frame-Options` is not `DENY` or `SAMEORIGIN` (any case) |
| `nosniff-missing` | `X-Content-Type-Options` is not `nosniff` (any case) |
| `referrer-policy-missing` | no `Referrer-Policy` |
| `referrer-policy-weak` | its **last** comma-separated value is `unsafe-url` or `no-referrer-when-downgrade` |
| `version-leak` | a `Server` header containing a digit, or any `X-Powered-By` header |
| `cookie-not-secure:NAME` | a `Set-Cookie` without the `Secure` attribute |
| `cookie-not-httponly:NAME` | a `Set-Cookie` without `HttpOnly` |
| `cookie-no-samesite:NAME` | a `Set-Cookie` without `SameSite=Lax`, `Strict` or `None` (any case) |

The CSP is a `;`-separated list of directives; each is a name (case-insensitive) followed by
space-separated sources (compared ignoring case). Only the first occurrence of a directive
counts. The **script sources** are those of `script-src`, or of `default-src` if there is no
`script-src`; the **object sources** likewise come from `object-src` or `default-src`.

Cookie findings come in cookie order, and for each cookie in the order of the table.
`NAME` is the text before the first `=`, stripped. Attribute names are case-insensitive.

## `secure_headers(nonce) -> list[tuple[str, str]]`

The header set a page should send, exactly:

```python
[
    ("Strict-Transport-Security", "max-age=63072000; includeSubDomains"),
    ("Content-Security-Policy", "default-src 'self'; script-src 'nonce-NONCE' 'strict-dynamic'; "
                                "object-src 'none'; base-uri 'none'; frame-ancestors 'none'"),
    ("X-Content-Type-Options", "nosniff"),
    ("Referrer-Policy", "strict-origin-when-cross-origin"),
    ("Permissions-Policy", "camera=(), microphone=(), geolocation=()"),
]
```

`NONCE` is the argument. It goes inside a header, so validate it first: it must be at least
16 characters of Base64 or Base64url (`A-Z a-z 0-9 + / - _`, optionally ending in up to two
`=`); otherwise raise `ValueError`. `check_headers(secure_headers(nonce))` returns `[]`.

Run the visible tests with `uv run pytest`. There are 13 tests: 2 in
`tests/test_visible.py` and 11 hidden ones, plus ruff on `headers.py`.
