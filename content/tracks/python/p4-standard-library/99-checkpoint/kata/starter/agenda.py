"""Import meeting exports from an inbox folder into one agenda: files, text and time zones."""

from collections.abc import Iterable
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path

REQUIRED = ("title", "date", "start", "duration", "zone")


@dataclass(frozen=True, slots=True)
class Event:
    title: str
    start: datetime  # aware, UTC
    end: datetime  # aware, UTC
    zone: str
    source: str


@dataclass(frozen=True, slots=True)
class Rejected:
    file: str
    line: int
    reason: str


@dataclass(frozen=True, slots=True)
class ImportResult:
    events: tuple[Event, ...]
    rejected: tuple[Rejected, ...]


def import_inbox(inbox: Path) -> ImportResult:
    """Read every *.csv directly in `inbox`, in name order, into one sorted agenda."""
    raise NotImplementedError("todo: write import_inbox()")


def write_agenda(result: ImportResult, target: Path) -> None:
    """Write the agenda as JSON, atomically: readers see the old file or the new one."""
    raise NotImplementedError("todo: write write_agenda()")


def weekly_counts(events: Iterable[Event], zone: str) -> dict[tuple[int, int], int]:
    """How many events start in each ISO (year, week), by the local date in `zone`."""
    raise NotImplementedError("todo: write weekly_counts()")
