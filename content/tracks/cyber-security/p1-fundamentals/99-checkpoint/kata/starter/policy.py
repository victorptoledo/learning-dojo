"""Your password policy from First Steps. Do not change this file."""

import unicodedata

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


def _normalise(value: str) -> str:
    return unicodedata.normalize("NFKC", value)


def _fold(value: str) -> str:
    return _normalise(value).casefold()


def check_password(
    password: str,
    *,
    username: str = "",
    mfa: bool = False,
    blocklist: frozenset[str] | set[str] = COMMON_PASSWORDS,
) -> list[str]:
    """The problems found in password, in a fixed order; [] when it is acceptable."""
    text = _normalise(password)
    folded = text.casefold()
    problems = []
    if len(text) < (MIN_LENGTH_WITH_MFA if mfa else MIN_LENGTH):
        problems.append("too-short")
    if len(text) > MAX_LENGTH:
        problems.append("too-long")
    if folded in {_fold(entry) for entry in blocklist}:
        problems.append("common")
    name = _fold(username)
    if len(name) >= 3 and name in folded:
        problems.append("contains-username")
    if len(set(text)) == 1:
        problems.append("repetitive")
    return problems


def is_acceptable(password: str, **options) -> bool:
    """True when check_password finds no problem."""
    return not check_password(password, **options)
