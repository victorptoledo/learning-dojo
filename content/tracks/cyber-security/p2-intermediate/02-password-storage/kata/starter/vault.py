"""Password storage. Today: unsalted SHA-256. Move it to salted scrypt."""

import hashlib

N, R, P = 2**14, 8, 1


def hash_password(password: str, *, n: int = N, r: int = R, p: int = P) -> str:
    """A new hash for password."""
    return "sha256$" + hashlib.sha256(password.encode()).hexdigest()


def verify_password(password: str, stored: str) -> bool:
    """True if password matches the stored hash; never raises."""
    return hash_password(password) == stored


def needs_rehash(stored: str, *, n: int = N, r: int = R, p: int = P) -> bool:
    """True if stored should be replaced by a hash with today's parameters."""
    return False


def login(
    password: str, stored: str, *, n: int = N, r: int = R, p: int = P
) -> tuple[bool, str | None]:
    """(matches, a replacement hash when one is needed)."""
    return verify_password(password, stored), None
