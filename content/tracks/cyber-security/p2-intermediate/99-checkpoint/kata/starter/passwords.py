"""Password hashing from the Password Storage kata. Do not change this file."""

import base64
import binascii
import hashlib
import hmac
import re
import secrets
import unicodedata

N, R, P = 2**14, 8, 1
SALT_BYTES = 16
HASH_BYTES = 32
MAX_N = 2**20
MAX_MEMORY = 64 * 1024 * 1024
PARAMS = re.compile(r"n=(\d+),r=(\d+),p=(\d+)")
HEX64 = re.compile(r"[0-9a-f]{64}")


def _normalise(password: str) -> bytes:
    return unicodedata.normalize("NFKC", password).encode("utf-8")


def _b64(data: bytes) -> str:
    return base64.b64encode(data).decode("ascii").rstrip("=")


def _unb64(text: str) -> bytes:
    if "=" in text:
        raise ValueError("padding")
    return base64.b64decode(text + "=" * (-len(text) % 4), validate=True)


def _parse(stored: str):
    """("sha256", hex) or ("scrypt", n, r, p, salt, hash); None when malformed."""
    try:
        if stored.startswith("sha256$"):
            digest = stored.removeprefix("sha256$")
            return ("sha256", digest) if HEX64.fullmatch(digest) else None
        parts = stored.split("$")
        if len(parts) != 4 or parts[0] != "scrypt":
            return None
        match = PARAMS.fullmatch(parts[1])
        if match is None:
            return None
        n, r, p = (int(value) for value in match.groups())
        if not (1 < n <= MAX_N and n & (n - 1) == 0 and 1 <= r <= 32 and 1 <= p <= 16):
            return None
        salt, digest = _unb64(parts[2]), _unb64(parts[3])
        if len(salt) < SALT_BYTES or len(digest) != HASH_BYTES:
            return None
        return ("scrypt", n, r, p, salt, digest)
    except (ValueError, TypeError, AttributeError, binascii.Error):
        return None


def hash_password(password: str, *, n: int = N, r: int = R, p: int = P) -> str:
    """A new hash for password."""
    salt = secrets.token_bytes(SALT_BYTES)
    digest = hashlib.scrypt(
        _normalise(password), salt=salt, n=n, r=r, p=p, maxmem=MAX_MEMORY, dklen=HASH_BYTES
    )
    return f"scrypt$n={n},r={r},p={p}${_b64(salt)}${_b64(digest)}"


def verify_password(password: str, stored: str) -> bool:
    """True if password matches the stored hash; never raises."""
    parsed = _parse(stored)
    if parsed is None or not isinstance(password, str):
        return False
    if parsed[0] == "sha256":
        actual = hashlib.sha256(_normalise(password)).hexdigest()
        return hmac.compare_digest(parsed[1], actual)
    _, n, r, p, salt, expected = parsed
    try:
        actual = hashlib.scrypt(
            _normalise(password), salt=salt, n=n, r=r, p=p, maxmem=MAX_MEMORY, dklen=len(expected)
        )
    except ValueError:
        return False
    return hmac.compare_digest(expected, actual)


def needs_rehash(stored: str, *, n: int = N, r: int = R, p: int = P) -> bool:
    """True if stored should be replaced by a hash with today's parameters."""
    parsed = _parse(stored)
    if parsed is None or parsed[0] != "scrypt":
        return True
    return parsed[1:4] != (n, r, p)


def login(
    password: str, stored: str, *, n: int = N, r: int = R, p: int = P
) -> tuple[bool, str | None]:
    """(matches, a replacement hash when one is needed)."""
    if not verify_password(password, stored):
        return False, None
    if needs_rehash(stored, n=n, r=r, p=p):
        return True, hash_password(password, n=n, r=r, p=p)
    return True, None


# A hash of a random password, checked when a username does not exist so that unknown and
# known users take the same time.
DUMMY_HASH = hash_password(secrets.token_urlsafe(16))
