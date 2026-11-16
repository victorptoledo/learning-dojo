"""Account sign-up with validation, bound SQL, a fixed role and encoded output."""

import html
import re
import sqlite3

from passwords import hash_password
from policy import check_password

FIELDS = ("username", "email", "display_name", "password")
USERNAME = re.compile(r"[A-Za-z0-9_.-]{3,30}")
MAX_EMAIL = 254
MAX_DISPLAY_NAME = 50


def _has_control(value: str) -> bool:
    return any(ord(char) < 32 or ord(char) == 127 for char in value)


def _valid_email(value: str) -> bool:
    if len(value) > MAX_EMAIL or _has_control(value) or any(c.isspace() for c in value):
        return False
    if value.count("@") != 1:
        return False
    local, domain = value.split("@")
    return bool(local) and "." in domain and not domain.startswith(".") and not domain.endswith(".")


def _valid_display_name(value: str) -> bool:
    name = value.strip()
    return 0 < len(name) <= MAX_DISPLAY_NAME and not _has_control(name)


def validate_signup(form: dict) -> dict[str, str]:
    """Field -> error code for every bad field; {} when the form is valid."""
    errors = {key: "unexpected" for key in form if key not in FIELDS}
    for field in FIELDS:
        value = form.get(field)
        if not isinstance(value, str) or value == "":
            errors[field] = "required"
    if "username" not in errors and USERNAME.fullmatch(form["username"]) is None:
        errors["username"] = "invalid"
    if "email" not in errors and not _valid_email(form["email"]):
        errors["email"] = "invalid"
    if "display_name" not in errors and not _valid_display_name(form["display_name"]):
        errors["display_name"] = "invalid"
    if "password" not in errors:
        username = form["username"] if isinstance(form.get("username"), str) else ""
        problems = check_password(form["password"], username=username)
        if problems:
            errors["password"] = problems[0]
    return errors


def register(conn: sqlite3.Connection, form: dict) -> tuple[int | None, dict[str, str]]:
    """Create the account: (new id, {}) or (None, errors)."""
    errors = validate_signup(form)
    if errors:
        return None, errors
    taken = conn.execute(
        "SELECT 1 FROM accounts WHERE username = ? COLLATE NOCASE", (form["username"],)
    ).fetchone()
    if taken:
        return None, {"username": "taken"}
    cursor = conn.execute(
        "INSERT INTO accounts (username, email, display_name, password_hash, role) "
        "VALUES (?, ?, ?, ?, 'member')",
        (
            form["username"],
            form["email"],
            form["display_name"].strip(),
            hash_password(form["password"]),
        ),
    )
    conn.commit()
    return cursor.lastrowid, {}


def render_welcome(conn: sqlite3.Connection, account_id: int) -> str:
    """The welcome banner for an account."""
    row = conn.execute(
        "SELECT display_name, username FROM accounts WHERE id = ?", (account_id,)
    ).fetchone()
    if row is None:
        raise LookupError(f"no account {account_id}")
    name, username = (html.escape(value) for value in row)
    return f"<h1>Welcome, {name}!</h1><p>Signed in as @{username}</p>"
