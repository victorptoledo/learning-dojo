"""Password policy checks for new and changed passwords."""

import string

MIN_LENGTH = 15
MIN_LENGTH_WITH_MFA = 8
MAX_LENGTH = 128

# A tiny sample of the most common passwords. A real service checks a large list of
# breached passwords (for example a local copy of a k-anonymity breach corpus).
COMMON_PASSWORDS = frozenset(
    {
        "123456789012345",
        "password",
        "password123456789",
        "qwertyuiopasdfgh",
        "iloveyouiloveyou",
        "letmein-letmein",
        "welcome12345678",
        "administrator123",
        "correcthorsebatterystaple",
    }
)


def check_password(
    password: str,
    *,
    username: str = "",
    mfa: bool = False,
    blocklist: frozenset[str] | set[str] = COMMON_PASSWORDS,
) -> list[str]:
    """The problems found in password, in a fixed order; [] when it is acceptable."""
    # Legacy policy: composition rules and a short maximum. Replace it.
    problems = []
    if len(password) < 8:
        problems.append("too-short")
    if len(password) > 16:
        problems.append("too-long")
    if not any(c.isupper() for c in password):
        problems.append("no-uppercase")
    if not any(c.isdigit() for c in password):
        problems.append("no-digit")
    if not any(c in string.punctuation for c in password):
        problems.append("no-symbol")
    return problems


def is_acceptable(password: str, **options) -> bool:
    """True when check_password finds no problem."""
    return not check_password(password, **options)
