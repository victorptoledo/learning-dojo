"""Access tokens for internal APIs. Signed, but never really checked: fix verify and more."""

import base64
import hashlib
import hmac
import json
import time

from passwords import verify_password

LEEWAY = 30


def _b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).decode("ascii").rstrip("=")


def _unb64(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


class TokenService:
    def __init__(self, keys, current_kid, *, issuer, clock=time.time):
        self._keys = keys
        self._current = current_kid
        self._issuer = issuer
        self._clock = clock

    def issue(self, subject: str, audience: str, ttl: int = 900) -> str:
        now = int(self._clock())
        claims = {"iss": self._issuer, "aud": audience, "sub": subject, "iat": now, "exp": now + ttl}
        payload = _b64(json.dumps(claims, separators=(",", ":"), sort_keys=True).encode())
        signed = f"v1.{self._current}.{payload}"
        signature = hmac.new(self._keys[self._current], signed.encode(), hashlib.sha256).digest()
        return f"{signed}.{_b64(signature)}"

    def verify(self, token: str, audience: str) -> dict | None:
        parts = token.split(".")
        claims = json.loads(_unb64(parts[2]))
        if claims["exp"] < self._clock():
            return None
        return claims

    def rotate(self, kid: str, key: bytes) -> None:
        self._keys[kid] = key
        self._current = kid

    def retire(self, kid: str) -> None:
        del self._keys[kid]


def login(service, users, username, password, audience):
    """A token for the user if the password is right, else None."""
    stored = users.get(username)
    if stored is None:
        return None
    if verify_password(password, stored):
        return service.issue(username, audience)
    return None
