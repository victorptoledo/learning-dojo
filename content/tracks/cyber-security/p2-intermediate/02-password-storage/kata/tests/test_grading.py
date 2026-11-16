import base64
import hashlib
import hmac
import re
import time

import vault
from vault import hash_password, login, needs_rehash, verify_password

FORMAT = re.compile(r"scrypt\$n=(\d+),r=(\d+),p=(\d+)\$([A-Za-z0-9+/]+)\$([A-Za-z0-9+/]+)")
PASSWORD = "correct horse battery staple"


def b64(data):
    return base64.b64encode(data).decode().rstrip("=")


def unb64(text):
    return base64.b64decode(text + "=" * (-len(text) % 4))


def legacy(password):
    return "sha256$" + hashlib.sha256(password.encode()).hexdigest()


def scrypt_value(password, *, n=2**14, r=8, p=1, salt=b"s" * 16):
    digest = hashlib.scrypt(password.encode(), salt=salt, n=n, r=r, p=p, dklen=32)
    return f"scrypt$n={n},r={r},p={p}${b64(salt)}${b64(digest)}"


def test_format_salt_and_hash_sizes():
    match = FORMAT.fullmatch(hash_password(PASSWORD))
    assert match is not None
    assert match.group(1, 2, 3) == ("16384", "8", "1")
    assert len(unb64(match.group(4))) == 16
    assert len(unb64(match.group(5))) == 32
    assert "=" not in match.group(4) + match.group(5)


def test_the_hash_is_real_scrypt_of_the_password():
    stored = hash_password(PASSWORD, n=2**12, r=8, p=1)
    _, params, salt, digest = stored.split("$")
    assert params == "n=4096,r=8,p=1"
    expected = hashlib.scrypt(PASSWORD.encode(), salt=unb64(salt), n=4096, r=8, p=1, dklen=32)
    assert unb64(digest) == expected


def test_salts_are_unique():
    salts = {hash_password(PASSWORD, n=2**10).split("$")[2] for _ in range(20)}
    assert len(salts) == 20


def test_verifies_values_from_other_implementations_and_nfkc():
    assert verify_password(PASSWORD, scrypt_value(PASSWORD))
    assert verify_password(PASSWORD, scrypt_value(PASSWORD, n=2**11, r=4, p=2))
    assert not verify_password(PASSWORD.upper(), scrypt_value(PASSWORD))
    stored = hash_password("ﬁle cabinet key")
    assert verify_password("file cabinet key", stored)


def test_legacy_hashes_still_verify():
    assert verify_password("hunter2-hunter2", legacy("hunter2-hunter2"))
    assert not verify_password("hunter3-hunter3", legacy("hunter2-hunter2"))


def test_malformed_values_never_verify_and_never_raise():
    good = scrypt_value(PASSWORD)
    _, params, salt, digest = good.split("$")
    bad = [
        "",
        "scrypt",
        "plain-text-password",
        "sha256$" + "A" * 64,
        "sha256$abc",
        f"scrypt${params}${salt}",
        f"scrypt${params}${salt}${digest}$extra",
        f"bcrypt${params}${salt}${digest}",
        f"scrypt$r=8,n=16384,p=1${salt}${digest}",
        f"scrypt$n=16384,r=8${salt}${digest}",
        f"scrypt$n=abc,r=8,p=1${salt}${digest}",
        f"scrypt${params}$!!!!${digest}",
        f"scrypt${params}${salt}==${digest}",
        f"scrypt${params}${b64(b'short')}${digest}",
        f"scrypt${params}${salt}${b64(b'x' * 31)}",
        None,
        42,
    ]
    for stored in bad:
        assert verify_password(PASSWORD, stored) is False, stored


def test_extreme_parameters_are_refused_quickly():
    salt, digest = b64(b"s" * 16), b64(b"d" * 32)
    started = time.monotonic()
    for params in ["n=1073741824,r=8,p=1", "n=1000,r=8,p=1", "n=1,r=8,p=1", "n=16384,r=64,p=1",
                   "n=16384,r=8,p=99", "n=16384,r=0,p=1", "n=2097152,r=8,p=1"]:
        assert verify_password(PASSWORD, f"scrypt${params}${salt}${digest}") is False, params
    assert time.monotonic() - started < 2


def test_comparisons_are_constant_time(monkeypatch):
    calls = []
    real = hmac.compare_digest

    def spy(a, b):
        calls.append((a, b))
        return real(a, b)

    monkeypatch.setattr(hmac, "compare_digest", spy)
    monkeypatch.setattr("secrets.compare_digest", spy)
    if hasattr(vault, "compare_digest"):
        monkeypatch.setattr(vault, "compare_digest", spy)
    assert verify_password(PASSWORD, scrypt_value(PASSWORD))
    assert verify_password("hunter2-hunter2", legacy("hunter2-hunter2"))
    assert len(calls) >= 2


def test_needs_rehash():
    assert needs_rehash(legacy("x"))
    assert needs_rehash("garbage")
    assert needs_rehash(scrypt_value(PASSWORD, n=2**12))
    assert needs_rehash(scrypt_value(PASSWORD, r=4))
    assert not needs_rehash(scrypt_value(PASSWORD))
    assert not needs_rehash(scrypt_value(PASSWORD, n=2**12), n=2**12)
    assert needs_rehash(scrypt_value(PASSWORD), n=2**15)


def test_login_upgrades_legacy_and_weak_hashes():
    ok, upgraded = login("hunter2-hunter2", legacy("hunter2-hunter2"))
    assert ok is True
    assert upgraded.startswith("scrypt$n=16384,r=8,p=1$")
    assert verify_password("hunter2-hunter2", upgraded)
    ok, upgraded = login(PASSWORD, scrypt_value(PASSWORD, n=2**12), n=2**13)
    assert ok is True
    assert upgraded.startswith("scrypt$n=8192,r=8,p=1$")


def test_login_without_upgrade_and_with_a_wrong_password():
    assert login(PASSWORD, scrypt_value(PASSWORD)) == (True, None)
    assert login("wrong", scrypt_value(PASSWORD)) == (False, None)
    assert login("wrong", legacy("hunter2-hunter2")) == (False, None)
    assert login(PASSWORD, "garbage") == (False, None)
