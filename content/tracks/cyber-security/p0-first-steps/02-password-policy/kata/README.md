# Password Policy

`policy.py` holds a legacy password policy: at least one upper-case letter, one digit and one
symbol, at most 16 characters. Rules like these push people to `Password1!`, block password
managers and long passphrases, and stop nobody. NIST SP 800-63B says: **no composition
rules**, a long minimum length, allow long passwords and any Unicode, and **reject passwords
that are known to be common or compromised**.

Rewrite `check_password` so that it returns the list of problems found, in this order:

| Code | When |
| --- | --- |
| `too-short` | fewer than `MIN_LENGTH` (15) characters, or `MIN_LENGTH_WITH_MFA` (8) when `mfa=True` |
| `too-long` | more than `MAX_LENGTH` (128) characters |
| `common` | the password is in `blocklist`, ignoring case |
| `contains-username` | `username` has 3 or more characters and appears inside the password, ignoring case |
| `repetitive` | the whole password is one character repeated (`aaaaaaaaaaaaaaaa`) |

An empty list means the password is acceptable. `is_acceptable(...)` returns `True` exactly
when there are no problems.

Rules for every comparison and count:

- First **normalise** the password (and `username`, and the blocklist entries) with Unicode
  **NFKC** (`unicodedata.normalize("NFKC", ...)`), so that `ﬁ` and `fi`, or a full-width `Ａ`
  and `A`, are treated the same. Count characters on the normalised text.
- "Ignoring case" means comparing `str.casefold()` of both sides.
- Spaces and any other characters are allowed and count as characters. Do **not** strip them.
- Never require upper case, digits or symbols.

```python
check_password("Tr0ub4dor&3")                          # ['too-short']
check_password("correct horse battery staple")         # []
check_password("PASSWORD1234567890", blocklist={"password1234567890"})  # ['common']
check_password("ada-lovelace-rules-ok", username="Ada")  # ['contains-username']
check_password("short1!", mfa=True)                    # ['too-short']  (7 < 8)
```

Run the visible tests with `uv run pytest`. There are 13 tests: 2 in
`tests/test_visible.py` and 11 hidden ones, plus ruff on `policy.py`.
