"""Password hashing with scrypt (phase 2 explains it). Do not change this file."""

import base64
import hashlib
import secrets

N, R, P = 2**14, 8, 1


def _b64(data: bytes) -> str:
    return base64.b64encode(data).decode("ascii").rstrip("=")


def hash_password(password: str) -> str:
    """A salted scrypt hash in the form scrypt$n=..,r=..,p=..$salt$hash."""
    salt = secrets.token_bytes(16)
    digest = hashlib.scrypt(password.encode("utf-8"), salt=salt, n=N, r=R, p=P, dklen=32)
    return f"scrypt$n={N},r={R},p={P}${_b64(salt)}${_b64(digest)}"
