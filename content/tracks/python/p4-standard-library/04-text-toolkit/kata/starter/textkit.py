"""Text helpers for a catalogue: search keys, slugs, highlighting and durations."""

from collections.abc import Iterable
from datetime import timedelta


def search_key(text: str) -> str:
    """A key that makes look-alike text compare equal: NFKC, casefold, single spaces."""
    raise NotImplementedError("todo: write search_key()")


def slugify(title: str, *, max_length: int = 60) -> str:
    """An ASCII slug: no accents, lower case, words joined by single hyphens."""
    raise NotImplementedError("todo: write slugify()")


def highlight(text: str, terms: Iterable[str], *, before: str = "[", after: str = "]") -> str:
    """Wrap every whole-word occurrence of any term, ignoring case, longest term first."""
    raise NotImplementedError("todo: write highlight()")


def parse_duration(text: str) -> timedelta:
    """Parse "1d 2h 30m 15s" (any subset, in this order) into a timedelta."""
    raise NotImplementedError("todo: write parse_duration() with re and unicodedata")

