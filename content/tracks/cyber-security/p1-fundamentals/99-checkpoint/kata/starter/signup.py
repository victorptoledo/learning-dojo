"""Account sign-up. It works for honest input and trusts everything else: fix it."""

import re
import sqlite3

from passwords import hash_password
from policy import check_password


def validate_signup(form: dict) -> dict[str, str]:
    """Field -> error code for every bad field; {} when the form is valid."""
    errors = {}
    if not re.match(r"^[A-Za-z0-9_.-]{3,30}$", form.get("username", "")):
        errors["username"] = "invalid"
    if "@" not in form.get("email", ""):
        errors["email"] = "invalid"
    problems = check_password(form.get("password", ""))
    if problems:
        errors["password"] = problems[0]
    return errors


def register(conn: sqlite3.Connection, form: dict) -> tuple[int | None, dict[str, str]]:
    """Create the account: (new id, {}) or (None, errors)."""
    errors = validate_signup(form)
    if errors:
        return None, errors
    taken = conn.execute(
        f"SELECT 1 FROM accounts WHERE username = '{form['username']}'"
    ).fetchone()
    if taken:
        return None, {"username": "taken"}
    role = form.get("role", "member")
    cursor = conn.execute(
        "INSERT INTO accounts (username, email, display_name, password_hash, role) "
        f"VALUES ('{form['username']}', '{form['email']}', '{form['display_name']}', "
        f"'{hash_password(form['password'])}', '{role}')"
    )
    conn.commit()
    return cursor.lastrowid, {}


def render_welcome(conn: sqlite3.Connection, account_id: int) -> str:
    """The welcome banner for an account."""
    row = conn.execute(
        f"SELECT display_name, username FROM accounts WHERE id = {account_id}"
    ).fetchone()
    return f"<h1>Welcome, {row[0]}!</h1><p>Signed in as @{row[1]}</p>"
