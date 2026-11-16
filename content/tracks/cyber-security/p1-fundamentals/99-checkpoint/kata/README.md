# Checkpoint 1: a Secure Sign-up

`signup.py` registers new accounts in SQLite. It works for honest input, and it is wide open:
SQL built with f-strings, the role taken from the form, a username check that lets a newline
through, and a welcome page that pastes user data into HTML. Fix it.

Given and already correct (do not change them): `db.py` (the schema), `policy.py` (your
password policy from First Steps) and `passwords.py` (password hashing, which phase 2
explains).

## `validate_signup(form) -> dict[str, str]`

`form` is a `dict` of submitted fields. Return a dict that maps each bad field to an error
code; `{}` means valid. Check, in this order (a field gets only its first error):

| Field | Code | When |
| --- | --- | --- |
| any key other than the four below | `unexpected` | for example `role` or `is_admin` (mass assignment) |
| `username`, `email`, `display_name`, `password` | `required` | missing, not a `str`, or `""` |
| `username` | `invalid` | not 3–30 characters of ASCII letters, digits, `_`, `.`, `-` (the whole value) |
| `email` | `invalid` | longer than 254 characters, contains whitespace or a control character, or is not exactly one `@` with a non-empty part before it and a part after it that contains a `.` not at either end |
| `display_name` | `invalid` | after `strip()` empty or longer than 50 characters, or contains a control character (below 32, or 127) |
| `password` | the **first** code of `policy.check_password(password, username=username)` | `too-short`, `common`, `contains-username`... |

Any characters are allowed in a display name otherwise: `O'Brien`, `<b>Ana</b>` and
`Zoë & Co` are valid names. Making them safe is the job of the output, not of validation.

## `register(conn, form) -> tuple[int | None, dict[str, str]]`

- If `validate_signup` finds errors, return `(None, errors)` and write nothing.
- If the username is taken, **ignoring case**, return `(None, {"username": "taken"})`.
- Otherwise insert the account with every value bound, the display name stripped, the
  password stored as `passwords.hash_password(password)` and the role **always**
  `member`. Return `(new_id, {})`.

## `render_welcome(conn, account_id) -> str`

```html
<h1>Welcome, DISPLAY_NAME!</h1><p>Signed in as @USERNAME</p>
```

Both values encoded with `html.escape`. An unknown id raises `LookupError`.

Run the visible tests with `uv run pytest`. There are 13 tests: 2 in
`tests/test_visible.py` and 11 hidden ones, plus ruff on every module.
