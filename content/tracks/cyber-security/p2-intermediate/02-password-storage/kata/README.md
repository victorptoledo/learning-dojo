# Password Storage

`vault.py` stores passwords as unsalted SHA-256 (`sha256$<hex digest>`). Replace it with
**scrypt** from `hashlib`, keeping support for the old hashes so existing users can still log
in, and upgrading them when they do.

Every password is first normalised with Unicode **NFKC** and encoded as UTF-8.

## The format

```text
scrypt$n=16384,r=8,p=1$<salt>$<hash>
```

`<salt>` is 16 random bytes from `secrets`, `<hash>` the 32-byte scrypt output, both in
standard Base64 **without** `=` padding. The defaults `N = 2**14, r = 8, p = 1` keep the tests
fast; production systems should use OWASP's `N = 2**17` (the functions take the parameters
as arguments).

## The functions

| Function | Does |
| --- | --- |
| `hash_password(password, *, n=N, r=R, p=P) -> str` | A new scrypt hash in the format above, with a fresh salt each time. |
| `verify_password(password, stored) -> bool` | `True` if the password matches `stored`, which may be `scrypt$...` or legacy `sha256$<64 lower-case hex>`. Compare with `hmac.compare_digest`. **Never raises**: anything malformed returns `False`. |
| `needs_rehash(stored, *, n=N, r=R, p=P) -> bool` | `True` for legacy hashes, malformed values and scrypt hashes whose `n`, `r` or `p` differ from the given ones. |
| `login(password, stored, *, n=N, r=R, p=P) -> tuple[bool, str \| None]` | `(False, None)` if the password is wrong; `(True, new_hash)` if it is right and `needs_rehash`; `(True, None)` otherwise. |

A stored scrypt value is malformed, so it never verifies, when: it does not have exactly four
`$`-separated parts; the parameters are not exactly `n=<int>,r=<int>,p=<int>` in that order;
`n` is not a power of two between 2 and 2^20; `r` is not between 1 and 32; `p` is not
between 1 and 16; the salt or the hash is not valid unpadded Base64; the salt is shorter
than 16 bytes; or the hash is not 32 bytes. The limits matter: a database row with
`n=2**30` would otherwise make one login request allocate gigabytes (pass
`maxmem=64 * 1024 * 1024` to `hashlib.scrypt`).

```python
stored = hash_password("correct horse battery staple")
verify_password("correct horse battery staple", stored)   # True
login("hunter2-hunter2-hunter2", "sha256$" + sha256_hex)  # (True, 'scrypt$n=16384,...')
verify_password("x", "scrypt$n=16384,r=8,p=1$!!!$???")    # False
```

Run the visible tests with `uv run pytest`. There are 13 tests: 2 in
`tests/test_visible.py` and 11 hidden ones, plus ruff on `vault.py`.
