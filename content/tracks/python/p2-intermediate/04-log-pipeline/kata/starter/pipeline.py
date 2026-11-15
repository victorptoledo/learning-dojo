"""A lazy pipeline over log lines: parse, filter, count per minute and batch."""

from collections.abc import Iterable, Iterator
from dataclasses import dataclass
from datetime import datetime

LEVELS = ("DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL")


@dataclass(frozen=True, slots=True)
class Entry:
    time: datetime
    level: str
    service: str
    message: str


def parse(lines: Iterable[str]) -> Iterator[Entry]:
    """Entries from log lines, lazily; blank, comment and malformed lines are skipped."""
    raise NotImplementedError("todo: write parse()")


def at_least(entries: Iterable[Entry], level: str) -> Iterator[Entry]:
    """Entries at level or above. An unknown level raises ValueError at the call."""
    raise NotImplementedError("todo: write at_least()")


def per_minute(entries: Iterable[Entry]) -> Iterator[tuple[str, int]]:
    """(minute, count) for each run of consecutive entries in the same minute."""
    raise NotImplementedError("todo: write per_minute()")


def batches(entries: Iterable[Entry], size: int) -> Iterator[list[Entry]]:
    """Lists of up to size entries. A size below 1 raises ValueError at the call."""
    raise NotImplementedError("todo: write batches()")


def busiest_service(entries: Iterable[Entry]) -> str | None:
    """The service with the most entries (ties: alphabetical), or None without entries."""
    raise NotImplementedError("todo: write busiest_service()")
