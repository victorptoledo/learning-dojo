"""Import meeting exports from an inbox folder into one agenda: files, text and time zones."""

import csv
import json
import os
import re
import tempfile
import unicodedata
from collections import Counter
from collections.abc import Iterable
from dataclasses import dataclass
from datetime import UTC, date, datetime, time, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

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


class RowError(ValueError):
    pass


_DATE = re.compile(r"\d{4}-\d{2}-\d{2}", re.ASCII)
_TIME = re.compile(r"(?P<hour>[01]\d|2[0-3]):(?P<minute>[0-5]\d)", re.ASCII)
_DURATION = re.compile(
    r"\s*(?:(?P<hours>\d+)\s*h\s*)?(?:(?P<minutes>\d+)\s*m\s*)?", re.ASCII | re.IGNORECASE
)


def _key(title: str) -> str:
    return " ".join(unicodedata.normalize("NFKC", title).casefold().split())


def _parse_date(text: str) -> date:
    if not _DATE.fullmatch(text):
        raise RowError("bad date")
    try:
        return date.fromisoformat(text)
    except ValueError:
        raise RowError("bad date") from None


def _parse_time(text: str) -> time:
    match = _TIME.fullmatch(text)
    if match is None:
        raise RowError("bad time")
    return time(int(match["hour"]), int(match["minute"]))


def _parse_duration(text: str) -> timedelta:
    match = _DURATION.fullmatch(text)
    if match is None or not any(match.groupdict().values()):
        raise RowError("bad duration")
    duration = timedelta(
        hours=int(match["hours"] or 0), minutes=int(match["minutes"] or 0)
    )
    if duration <= timedelta(0):
        raise RowError("bad duration")
    return duration


def _zone(name: str) -> ZoneInfo:
    if not name:
        raise RowError("unknown zone")
    try:
        return ZoneInfo(name)
    except (ZoneInfoNotFoundError, ValueError):
        raise RowError("unknown zone") from None


def _resolve(local: datetime) -> datetime:
    """The UTC instant of a local wall time, refusing times in a DST gap or fold."""
    first, second = local.replace(fold=0), local.replace(fold=1)
    if first.utcoffset() != second.utcoffset():
        back = first.astimezone(UTC).astimezone(local.tzinfo)
        if back.replace(tzinfo=None) != first.replace(tzinfo=None):
            raise RowError("nonexistent time")
        raise RowError("ambiguous time")
    return first.astimezone(UTC)


def _event(row: dict[str, str], source: str) -> Event:
    title = " ".join(unicodedata.normalize("NFC", row["title"]).split())
    if not title:
        raise RowError("empty title")
    day = _parse_date(row["date"].strip())
    at = _parse_time(row["start"].strip())
    duration = _parse_duration(row["duration"])
    zone_name = row["zone"].strip()
    zone = _zone(zone_name)
    start = _resolve(datetime.combine(day, at, tzinfo=zone))
    return Event(title, start, start + duration, zone_name, source)


def _read_file(path: Path) -> tuple[list[Event], list[Rejected]]:
    events: list[Event] = []
    rejected: list[Rejected] = []
    try:
        with path.open(encoding="utf-8-sig", newline="") as source:
            reader = csv.reader(source)
            header = next(reader, None)
            columns = [name.strip().casefold() for name in header or []]
            missing = [name for name in REQUIRED if name not in columns]
            if missing:
                return [], [Rejected(path.name, 1, f"missing column {missing[0]}")]
            for fields in reader:
                if not any(field.strip() for field in fields):
                    continue
                row = dict(zip(columns, fields, strict=False))
                row = {name: row.get(name) or "" for name in REQUIRED}
                try:
                    events.append(_event(row, path.name))
                except RowError as error:
                    rejected.append(Rejected(path.name, reader.line_num, str(error)))
    except UnicodeDecodeError:
        return [], [Rejected(path.name, 0, "not utf-8")]
    return events, rejected


def import_inbox(inbox: Path) -> ImportResult:
    """Read every *.csv directly in `inbox`, in name order, into one sorted agenda."""
    events: list[Event] = []
    rejected: list[Rejected] = []
    seen: set[tuple[datetime, str]] = set()
    for path in sorted(p for p in inbox.glob("*.csv") if p.is_file()):
        file_events, file_rejected = _read_file(path)
        rejected.extend(file_rejected)
        for event in file_events:
            identity = (event.start, _key(event.title))
            if identity in seen:
                continue
            seen.add(identity)
            events.append(event)
    events.sort(key=lambda event: (event.start, event.title))
    return ImportResult(tuple(events), tuple(rejected))


def write_agenda(result: ImportResult, target: Path) -> None:
    """Write the agenda as JSON, atomically: readers see the old file or the new one."""
    document = {
        "events": [
            {
                "title": event.title,
                "start": event.start.isoformat(),
                "end": event.end.isoformat(),
                "zone": event.zone,
                "source": event.source,
            }
            for event in result.events
        ],
        "rejected": [
            {"file": item.file, "line": item.line, "reason": item.reason}
            for item in result.rejected
        ],
    }
    text = json.dumps(document, ensure_ascii=False, indent=2) + "\n"
    with tempfile.NamedTemporaryFile(
        "w", encoding="utf-8", dir=target.parent, prefix=f".{target.name}.", delete=False
    ) as tmp:
        tmp.write(text)
    try:
        os.replace(tmp.name, target)
    except BaseException:
        os.unlink(tmp.name)
        raise


def weekly_counts(events: Iterable[Event], zone: str) -> dict[tuple[int, int], int]:
    """How many events start in each ISO (year, week), by the local date in `zone`."""
    tz = ZoneInfo(zone)
    counts: Counter[tuple[int, int]] = Counter()
    for event in events:
        iso = event.start.astimezone(tz).date().isocalendar()
        counts[(iso.year, iso.week)] += 1
    return dict(counts)
