# Checkpoint 2: Signed Tokens with Rotation

`tokens.py` logs users in and issues access tokens for internal APIs. Today the tokens are
signed but `verify` never checks the signature, the key or the audience. Rewrite it.

`passwords.py` is given and correct: `verify_password(password, stored)` and the
`DUMMY_HASH` constant, from the Password Storage kata. Do not change it.

## The token format

```text
v1.<kid>.<payload>.<signature>
```

- `<payload>` is urlsafe Base64 **without** `=` padding of the JSON claims, written with
  `json.dumps(claims, separators=(",", ":"), sort_keys=True)`: `iss`, `aud`, `sub`, `iat`,
  `exp` (`iat` and `exp` are whole seconds from `self._clock()`).
- `<signature>` is urlsafe Base64 without padding of
  `HMAC-SHA256(keys[kid], "v1." + kid + "." + payload)`, the ASCII text of the first three
  parts joined by dots.

## `TokenService(keys, current_kid, *, issuer, clock=time.time)`

`keys` maps key ids to secret keys (`bytes`); new tokens are signed with `current_kid`.
Raise `ValueError` if any key is shorter than 32 bytes, if a key id is empty or contains a
`.`, or if `current_kid` is not in `keys`. Copy the dict: later changes by the caller must
not affect the service.

| Method | Does |
| --- | --- |
| `issue(subject, audience, ttl=900) -> str` | A token for `subject` (a non-empty `str`), valid for `ttl` seconds; `ValueError` unless `1 <= ttl <= 3600` (an `int`, not a `bool`). |
| `verify(token, audience) -> dict \| None` | The claims if the token is valid for this `audience`, else `None`. Never raises. |
| `rotate(kid, key)` | Adds a key and makes it the current one (same rules as the constructor). |
| `retire(kid)` | Removes a key; tokens signed with it stop verifying. Retiring the current key or an unknown one raises `ValueError`. |

A token is valid only if: it has exactly four dot-separated parts and starts with `v1`; the
`kid` is known; the signature decodes and equals the expected HMAC under
`hmac.compare_digest`; the payload decodes to a JSON **object**; `iss` equals the service's
issuer and `aud` equals `audience`; `sub` is a non-empty string; `iat` and `exp` are
integers (not booleans); and `iat - LEEWAY <= now < exp + LEEWAY`, with `LEEWAY = 30`
seconds for clock skew. Padding (`=`) or characters outside the urlsafe alphabet make it
invalid.

## `login(service, users, username, password, audience) -> str | None`

`users` maps usernames to stored password hashes. Return `service.issue(username, audience)`
if the password verifies, otherwise `None`. When the username is unknown, still call
`verify_password(password, DUMMY_HASH)`, so that the response time does not reveal which
usernames exist.

Run the visible tests with `uv run pytest`. There are 14 tests: 2 in
`tests/test_visible.py` and 12 hidden ones, plus ruff on every module.
