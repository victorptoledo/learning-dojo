import pytest

from db import connect
from signup import register, render_welcome, validate_signup

FORM = {
    "username": "ada",
    "email": "ada@example.org",
    "display_name": "Ada Lovelace",
    "password": "analytical engine notes",
}


def form(**changes):
    result = {**FORM, **changes}
    return {key: value for key, value in result.items() if value is not ...}


def accounts(conn):
    return conn.execute(
        "SELECT username, email, display_name, role FROM accounts ORDER BY id"
    ).fetchall()


def test_a_valid_form_has_no_errors():
    assert validate_signup(FORM) == {}
    assert validate_signup(form(username="grace.hopper-1906", email="g.h@mail.navy.mil")) == {}


def test_unexpected_and_required_fields():
    assert validate_signup(form(role="admin", is_admin=True)) == {
        "role": "unexpected",
        "is_admin": "unexpected",
    }
    errors = validate_signup(form(email=..., display_name="", password=None))
    assert errors == {"email": "required", "display_name": "required", "password": "required"}
    assert validate_signup({}) == {field: "required" for field in FORM}
    assert validate_signup(form(username=42)) == {"username": "required"}


def test_usernames_must_match_entirely():
    for bad in ["ab", "ada\n", "ada lovelace", "ada'--", "x' OR '1'='1", "a" * 31]:
        assert validate_signup(form(username=bad)) == {"username": "invalid"}, bad


def test_emails():
    for bad in [
        "ada",
        "ada@",
        "@example.org",
        "ada@localhost",
        "ada@.org",
        "ada@org.",
        "a@b@example.org",
        "ada @example.org",
        "ada@example.org\n",
        "a" * 250 + "@x.io",
    ]:
        assert validate_signup(form(email=bad)) == {"email": "invalid"}, bad


def test_display_names_allow_any_text_but_not_control_characters():
    for good in ["O'Brien", "<b>Ana</b>", "Zoë & Co", "  padded  ", "x" * 50]:
        assert validate_signup(form(display_name=good)) == {}, good
    for bad in ["   ", "x" * 51, "Ana\nAdmin", "Ana\x00", "Ana\x7f"]:
        assert validate_signup(form(display_name=bad)) == {"display_name": "invalid"}, bad


def test_password_policy_is_applied_with_the_username():
    assert validate_signup(form(password="short")) == {"password": "too-short"}
    assert validate_signup(form(password="password123456789")) == {"password": "common"}
    assert validate_signup(form(password="ada is my name forever")) == {
        "password": "contains-username"
    }


def test_register_stores_a_member_with_a_hash():
    conn = connect()
    account_id, errors = register(conn, form(display_name="  Ada Lovelace  "))
    assert (account_id, errors) == (1, {})
    assert accounts(conn) == [("ada", "ada@example.org", "Ada Lovelace", "member")]
    stored = conn.execute("SELECT password_hash FROM accounts").fetchone()[0]
    assert stored.startswith("scrypt$")
    assert FORM["password"] not in stored


def test_invalid_forms_write_nothing():
    conn = connect()
    assert register(conn, form(role="admin")) == (None, {"role": "unexpected"})
    assert register(conn, form(username="ada\n")) == (None, {"username": "invalid"})
    assert accounts(conn) == []


def test_usernames_are_unique_ignoring_case():
    conn = connect()
    assert register(conn, FORM)[1] == {}
    assert register(conn, form(username="ADA", email="other@example.org")) == (
        None,
        {"username": "taken"},
    )
    assert register(conn, form(username="ada.l"))[1] == {}
    assert len(accounts(conn)) == 2


def test_values_are_bound_not_pasted():
    conn = connect()
    tricky = "Robert'); DROP TABLE accounts; --"
    account_id, errors = register(conn, form(display_name=tricky))
    assert errors == {}
    assert accounts(conn) == [("ada", "ada@example.org", tricky, "member")]
    second_id, _ = register(
        conn, form(username="eve", display_name="eve', 'x', 'admin') --", email="eve@example.org")
    )
    roles = [row[3] for row in accounts(conn)]
    assert roles == ["member", "member"]
    assert second_id == 2


def test_welcome_encodes_the_values():
    conn = connect()
    account_id, _ = register(conn, form(display_name='<img src=x onerror="alert(1)"> & Co'))
    assert render_welcome(conn, account_id) == (
        "<h1>Welcome, &lt;img src=x onerror=&quot;alert(1)&quot;&gt; &amp; Co!</h1>"
        "<p>Signed in as @ada</p>"
    )
    with pytest.raises(LookupError):
        render_welcome(conn, 99)
    with pytest.raises(LookupError):
        render_welcome(conn, "1 OR 1=1")
