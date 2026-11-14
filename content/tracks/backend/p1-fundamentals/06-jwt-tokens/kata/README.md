# JWT from Scratch

Implement `JwtService`: issue and verify **HS256** JSON Web Tokens. `MiniJson` (enough JSON for
headers and claims), `Claims` and `InvalidTokenException` are written.

## Issuing

```text
token = base64url(header) + "." + base64url(payload) + "." + base64url(signature)

header    = {"alg":"HS256","typ":"JWT"}
payload   = {"sub":"ada","exp":1794650400,"roles":["USER"]}     exp = now + ttl, in epoch seconds
signature = HMAC-SHA256(secret, base64url(header) + "." + base64url(payload))
```

**base64url** is Base64 with `-` and `_` instead of `+` and `/`, and **no `=` padding**:
`Base64.getUrlEncoder().withoutPadding()`. "Now" comes from the `Clock` given to the constructor,
so tests control time.

## Verifying

Check in this order and throw `InvalidTokenException` with the reason:

| Problem | Reason |
| --- | --- |
| `null`, not exactly three parts, a part that is not base64url, or a part that is not JSON | `malformed` |
| the header's `alg` is not `HS256` (for example `none`) | `unsupported algorithm` |
| the signature does not match (compare with `MessageDigest.isEqual`, in constant time) | `bad signature` |
| `sub` or `exp` missing | `malformed` |
| now is at or after `exp` | `expired` |

Otherwise return the `Claims` (`roles` is empty when the token has none).

The algorithm is checked **before** the signature, and only the one you expect is accepted:
letting the token choose its own algorithm is the classic JWT vulnerability.

> Tip: `Mac.getInstance("HmacSHA256")` with a `SecretKeySpec(secret, "HmacSHA256")`,
> `token.split("\\.", -1)` (keeps empty parts), `Instant.ofEpochSecond(exp)`, and
> `clock.instant()`.
