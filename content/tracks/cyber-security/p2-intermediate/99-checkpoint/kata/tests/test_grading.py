import base64
import hashlib
import hmac
import json

import pytest

import passwords
from passwords import hash_password
from tokens import LEEWAY, TokenService, login

NOW = 1_794_700_000
ISSUER = "https://auth.dojo.test"
K1 = b"1" * 32
K2 = b"2" * 40


def b64(data):
    return base64.urlsafe_b64encode(data).decode().rstrip("=")


def unb64(text):
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


class Clock:
    def __init__(self, now=NOW):
        self.now = now

    def __call__(self):
        return self.now


def service(clock=None, keys=None, current="k1"):
    return TokenService(keys or {"k1": K1}, current, issuer=ISSUER, clock=clock or Clock())


def forge(claims, kid="k1", key=K1):
    payload = b64(json.dumps(claims, separators=(",", ":"), sort_keys=True).encode())
    signature = hmac.new(key, f"v1.{kid}.{payload}".encode(), hashlib.sha256).digest()
    return f"v1.{kid}.{payload}.{b64(signature)}"


def signed_raw(payload_bytes, kid="k1", key=K1):
    payload = b64(payload_bytes)
    signature = hmac.new(key, f"v1.{kid}.{payload}".encode(), hashlib.sha256).digest()
    return f"v1.{kid}.{payload}.{b64(signature)}"


GOOD = {"iss": ISSUER, "aud": "orders-api", "sub": "ada", "iat": NOW, "exp": NOW + 900}


def test_issue_writes_the_documented_format():
    token = service().issue("ada", "orders-api", ttl=600)
    version, kid, payload, signature = token.split(".")
    assert (version, kid) == ("v1", "k1")
    assert "=" not in token
    assert json.loads(unb64(payload)) == {
        "aud": "orders-api", "exp": NOW + 600, "iat": NOW, "iss": ISSUER, "sub": "ada",
    }
    expected = hmac.new(K1, f"v1.k1.{payload}".encode(), hashlib.sha256).digest()
    assert unb64(signature) == expected
    assert token == forge({**GOOD, "exp": NOW + 600})


def test_issue_validates_its_arguments():
    for ttl in [0, -1, 3601, True, 1.5, "60"]:
        with pytest.raises(ValueError):
            service().issue("ada", "orders-api", ttl=ttl)
    for subject in ["", None, 42]:
        with pytest.raises(ValueError):
            service().issue(subject, "orders-api")
    assert service().verify(service().issue("ada", "a", ttl=3600), "a") is not None


def test_signatures_are_checked():
    good = forge(GOOD)
    assert service().verify(good, "orders-api") == GOOD
    version, kid, payload, signature = good.split(".")
    tampered_payload = b64(json.dumps({**GOOD, "sub": "admin"}, separators=(",", ":")).encode())
    assert service().verify(f"{version}.{kid}.{tampered_payload}.{signature}", "orders-api") is None
    assert service().verify(forge(GOOD, key=b"x" * 32), "orders-api") is None
    assert service().verify(f"{version}.{kid}.{payload}.", "orders-api") is None
    assert service().verify(f"{version}.{kid}.{payload}", "orders-api") is None


def test_unsigned_and_unknown_key_tokens_are_refused():
    payload = b64(json.dumps(GOOD).encode())
    assert service().verify(f"v1.k1.{payload}.", "orders-api") is None
    assert service().verify(forge(GOOD, kid="k9", key=K1), "orders-api") is None
    assert service().verify(forge(GOOD).replace("v1.", "v2.", 1), "orders-api") is None
    alg_none = b64(b'{"alg":"none"}') + "." + payload + "."
    assert service().verify(alg_none, "orders-api") is None


def test_claims_are_checked():
    s = service()
    assert s.verify(forge(GOOD), "billing-api") is None
    assert s.verify(forge({**GOOD, "iss": "https://evil.test"}), "orders-api") is None
    for broken in [
        {**GOOD, "sub": ""},
        {**GOOD, "sub": 7},
        {**GOOD, "exp": True},
        {**GOOD, "exp": float(NOW + 900)},
        {**GOOD, "iat": "now"},
        {key: value for key, value in GOOD.items() if key != "exp"},
        {key: value for key, value in GOOD.items() if key != "aud"},
    ]:
        assert s.verify(forge(broken), "orders-api") is None, broken
    assert s.verify(forge([1, 2, 3]), "orders-api") is None
    assert s.verify(forge("just a string"), "orders-api") is None


def test_expiry_and_issue_time_use_the_leeway():
    clock = Clock()
    s = service(clock)
    token = s.issue("ada", "orders-api", ttl=60)
    clock.now = NOW + 60 + LEEWAY - 1
    assert s.verify(token, "orders-api") is not None
    clock.now = NOW + 60 + LEEWAY
    assert s.verify(token, "orders-api") is None
    future = forge({**GOOD, "iat": NOW + LEEWAY + 5, "exp": NOW + 900})
    clock.now = NOW
    assert s.verify(future, "orders-api") is None
    assert s.verify(forge({**GOOD, "iat": NOW + LEEWAY}), "orders-api") is not None


def test_malformed_tokens_never_raise():
    s = service()
    good = forge(GOOD)
    _, _, payload, signature = good.split(".")
    for token in [
        "",
        "....",
        "v1",
        good + ".extra",
        f"v1.k1.{payload}.{signature}=",
        f"v1.k1.{payload}==.{signature}",
        f"v1.k1.{payload}.{signature[:-2]}+/",
        f"v1.k1.!!!.{signature}",
        signed_raw(b"\xff\xfe\x00garbage"),
        signed_raw(b"{not json"),
        None,
        12345,
    ]:
        assert s.verify(token, "orders-api") is None, token


def test_constructor_and_rotate_validate_keys():
    with pytest.raises(ValueError):
        TokenService({"k1": b"short"}, "k1", issuer=ISSUER)
    with pytest.raises(ValueError):
        TokenService({"k1": K1}, "k2", issuer=ISSUER)
    with pytest.raises(ValueError):
        TokenService({"a.b": K1}, "a.b", issuer=ISSUER)
    with pytest.raises(ValueError):
        TokenService({"": K1}, "", issuer=ISSUER)
    with pytest.raises(ValueError):
        service().rotate("k2", b"too short")
    with pytest.raises(ValueError):
        service().rotate("bad.id", K2)


def test_the_keys_dict_is_copied():
    keys = {"k1": K1}
    s = TokenService(keys, "k1", issuer=ISSUER, clock=Clock())
    keys["evil"] = b"e" * 32
    assert s.verify(forge(GOOD, kid="evil", key=b"e" * 32), "orders-api") is None


def test_rotation_add_switch_retire():
    s = service()
    old = s.issue("ada", "orders-api")
    s.rotate("k2", K2)
    new = s.issue("grace", "orders-api")
    assert new.split(".")[1] == "k2"
    assert s.verify(old, "orders-api")["sub"] == "ada"
    assert s.verify(new, "orders-api")["sub"] == "grace"
    s.retire("k1")
    assert s.verify(old, "orders-api") is None
    assert s.verify(new, "orders-api")["sub"] == "grace"
    with pytest.raises(ValueError):
        s.retire("k2")
    with pytest.raises(ValueError):
        s.retire("k1")


def test_login_checks_the_password():
    users = {"ada": hash_password("analytical engine notes")}
    s = service()
    token = login(s, users, "ada", "analytical engine notes", "orders-api")
    assert s.verify(token, "orders-api")["sub"] == "ada"
    assert login(s, users, "ada", "wrong password here", "orders-api") is None
    assert login(s, users, "nobody", "analytical engine notes", "orders-api") is None


def test_login_spends_the_same_work_on_unknown_users(monkeypatch):
    seen = []
    real = passwords.verify_password

    def spy(password, stored):
        seen.append(stored)
        return real(password, stored)

    monkeypatch.setattr(passwords, "verify_password", spy)
    import tokens

    if hasattr(tokens, "verify_password"):
        monkeypatch.setattr(tokens, "verify_password", spy)
    assert login(service(), {}, "nobody", "whatever password", "orders-api") is None
    assert seen == [passwords.DUMMY_HASH]
