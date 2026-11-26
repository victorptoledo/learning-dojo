"""Text helpers for a catalogue: search keys, slugs, highlighting and durations."""

import re
import unicodedata
from collections.abc import Iterable
from datetime import timedelta


def search_key(text: str) -> str:
    """A key that makes look-alike text compare equal: NFKC, casefold, single spaces."""
    return " ".join(unicodedata.normalize("NFKC", text).casefold().split())


def _strip_accents(text: str) -> str:
    decomposed = unicodedata.normalize("NFKD", text)
    return "".join(char for char in decomposed if unicodedata.category(char) != "Mn")


_NOT_SLUG = re.compile(r"[^a-z0-9]+")


def slugify(title: str, *, max_length: int = 60) -> str:
    """An ASCII slug: no accents, lower case, words joined by single hyphens."""
    if max_length < 1:
        raise ValueError("max_length must be at least 1")
    slug = _NOT_SLUG.sub("-", _strip_accents(title).casefold()).strip("-")
    if not slug:
        raise ValueError(f"nothing to slugify in {title!r}")
    if len(slug) > max_length:
        cut = slug[:max_length]
        if slug[max_length] != "-" and "-" in cut:
            cut = cut[: cut.rindex("-")]
        slug = cut.rstrip("-")
    return slug


def highlight(text: str, terms: Iterable[str], *, before: str = "[", after: str = "]") -> str:
    """Wrap every whole-word occurrence of any term, ignoring case, longest term first."""
    unique = {term.strip() for term in terms if term.strip()}
    if not unique:
        return text
    alternatives = "|".join(re.escape(term) for term in sorted(unique, key=lambda t: (-len(t), t)))
    pattern = re.compile(rf"(?<!\w)(?:{alternatives})(?!\w)", re.IGNORECASE)
    return pattern.sub(lambda match: f"{before}{match[0]}{after}", text)


_DURATION = re.compile(
    r"""
    \s*
    (?:(?P<days>\d+)\s*d\s*)?
    (?:(?P<hours>\d+)\s*h\s*)?
    (?:(?P<minutes>\d+)\s*m\s*)?
    (?:(?P<seconds>\d+)\s*s\s*)?
    """,
    re.VERBOSE | re.IGNORECASE | re.ASCII,
)


def parse_duration(text: str) -> timedelta:
    """Parse "1d 2h 30m 15s" (any subset, in this order) into a timedelta."""
    match = _DURATION.fullmatch(text)
    if match is None or not any(match.groupdict().values()):
        raise ValueError(f"not a duration: {text!r}")
    parts = {unit: int(value) for unit, value in match.groupdict().items() if value}
    return timedelta(**parts)
