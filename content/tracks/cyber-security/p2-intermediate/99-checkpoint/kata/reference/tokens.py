"""Access tokens for internal APIs: HMAC-signed, fully validated, with key rotation."""

import base64
import binascii
import hashlib
import hmac
import json
import re
import time

import passwords

LEEWAY = 30
MIN_KEY_BYTES = 32
MAX_TTL = 3600
URLSAFE = re.compile(r"[A-Za-z0-9_-]*")


def _b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")


def _unb64(text: str) -> bytes:
    if URLSAFE.fullmatch(text) is None:
        raise ValueError("not unpadded urlsafe base64")
    try:
        return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))
    except binascii.Error as error:
        raise ValueError("bad base64") from error


def _check_key(kid: object, key: object) -> None:
    if not isinstance(kid, str) or kid == "" or "." in kid:
        raise ValueError(f"invalid key id: {kid!r}")
    if not isinstance(key, bytes) or len(key) < MIN_KEY_BYTES:
        raise ValueError(f"key {kid!r} must be at least {MIN_KEY_BYTES} bytes")


def _is_int(value: object) -> bool:
    return type(value) is int


class TokenService:
    def __init__(self, keys, current_kid, *, issuer, clock=time.time):
        for kid, key in keys.items():
            _check_key(kid, key)
        if current_kid not in keys:
            raise ValueError("the current key id must be one of the keys")
        self._keys = dict(keys)
        self._current = current_kid
        self._issuer = issuer
        self._clock = clock

    def _sign(self, kid: str, payload: str) -> bytes:
        return hmac.new(self._keys[kid], f"v1.{kid}.{payload}".encode("ascii"), hashlib.sha256).digest()

    def issue(self, subject: str, audience: str, ttl: int = 900) -> str:
        if not isinstance(subject, str) or subject == "":
            raise ValueError("subject must be a non-empty string")
        if not _is_int(ttl) or not 1 <= ttl <= MAX_TTL:
            raise ValueError(f"ttl must be an int between 1 and {MAX_TTL}")
        now = int(self._clock())
        claims = {"iss": self._issuer, "aud": audience, "sub": subject, "iat": now, "exp": now + ttl}
        payload = _b64(json.dumps(claims, separators=(",", ":"), sort_keys=True).encode("utf-8"))
        return f"v1.{self._current}.{payload}.{_b64(self._sign(self._current, payload))}"

    def verify(self, token: str, audience: str) -> dict | None:
        if not isinstance(token, str):
            return None
        parts = token.split(".")
        if len(parts) != 4 or parts[0] != "v1" or parts[1] not in self._keys:
            return None
        _, kid, payload, signature_text = parts
        try:
            signature = _unb64(signature_text)
            if not hmac.compare_digest(signature, self._sign(kid, payload)):
                return None
            claims = json.loads(_unb64(payload))
        except (ValueError, UnicodeError):
            return None
        if not isinstance(claims, dict):
            return None
        if claims.get("iss") != self._issuer or claims.get("aud") != audience:
            return None
        subject, issued, expires = claims.get("sub"), claims.get("iat"), claims.get("exp")
        if not isinstance(subject, str) or subject == "" or not _is_int(issued) or not _is_int(expires):
            return None
        now = self._clock()
        if not issued - LEEWAY <= now < expires + LEEWAY:
            return None
        return claims

    def rotate(self, kid: str, key: bytes) -> None:
        _check_key(kid, key)
        self._keys[kid] = key
        self._current = kid

    def retire(self, kid: str) -> None:
        if kid == self._current or kid not in self._keys:
            raise ValueError(f"cannot retire {kid!r}")
        del self._keys[kid]


def login(service, users, username, password, audience):
    """A token for the user if the password is right, else None."""
    stored = users.get(username)
    if stored is None:
        passwords.verify_password(password, passwords.DUMMY_HASH)
        return None
    if passwords.verify_password(password, stored):
        return service.issue(username, audience)
    return None
