from db import connect
from signup import register, render_welcome

FORM = {
    "username": "ada",
    "email": "ada@example.org",
    "display_name": "Ada Lovelace",
    "password": "analytical engine notes",
}


def test_an_honest_sign_up_works():
    conn = connect()
    account_id, errors = register(conn, FORM)
    assert errors == {}
    assert render_welcome(conn, account_id) == (
        "<h1>Welcome, Ada Lovelace!</h1><p>Signed in as @ada</p>"
    )


def test_the_role_never_comes_from_the_form():
    account_id, errors = register(connect(), {**FORM, "role": "admin"})
    assert account_id is None
    assert errors == {"role": "unexpected"}
