"""A lazy pipeline over log lines: parse, filter, count per minute and batch."""

from collections.abc import Iterable, Iterator
from dataclasses import dataclass
from datetime import datetime
from itertools import batched, groupby

LEVELS = ("DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL")


@dataclass(frozen=True, slots=True)
class Entry:
    time: datetime
    level: str
    service: str
    message: str


def parse(lines: Iterable[str]) -> Iterator[Entry]:
    """Entries from log lines, lazily; blank, comment and malformed lines are skipped."""
    for line in lines:
        text = line.strip()
        if not text or text.startswith("#"):
            continue
        parts = text.split(maxsplit=3)
        if len(parts) < 3 or parts[1] not in LEVELS:
            continue
        try:
            time = datetime.fromisoformat(parts[0])
        except ValueError:
            continue
        message = parts[3] if len(parts) == 4 else ""
        yield Entry(time, parts[1], parts[2], message)


def at_least(entries: Iterable[Entry], level: str) -> Iterator[Entry]:
    """Entries at level or above. An unknown level raises ValueError at the call."""
    if level not in LEVELS:
        raise ValueError(f"unknown level: {level!r}")
    minimum = LEVELS.index(level)
    return (entry for entry in entries if LEVELS.index(entry.level) >= minimum)


def per_minute(entries: Iterable[Entry]) -> Iterator[tuple[str, int]]:
    """(minute, count) for each run of consecutive entries in the same minute."""
    for minute, group in groupby(entries, key=lambda entry: entry.time.strftime("%Y-%m-%dT%H:%M")):
        yield minute, sum(1 for _ in group)


def batches(entries: Iterable[Entry], size: int) -> Iterator[list[Entry]]:
    """Lists of up to size entries. A size below 1 raises ValueError at the call."""
    if size < 1:
        raise ValueError("size must be at least 1")
    return (list(batch) for batch in batched(entries, size))


def busiest_service(entries: Iterable[Entry]) -> str | None:
    """The service with the most entries (ties: alphabetical), or None without entries."""
    counts: dict[str, int] = {}
    for entry in entries:
        counts[entry.service] = counts.get(entry.service, 0) + 1
    if not counts:
        return None
    return min(counts, key=lambda service: (-counts[service], service))
