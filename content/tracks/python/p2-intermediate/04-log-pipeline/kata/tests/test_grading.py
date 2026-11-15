from collections.abc import Iterator
from datetime import UTC, datetime, timedelta
from itertools import count, islice
from pathlib import Path

import pytest

from pipeline import Entry, at_least, batches, busiest_service, parse, per_minute

SAMPLE = Path(__file__).parent.parent / "sample.log"


def sample_entries():
    with SAMPLE.open(encoding="utf-8") as lines:
        return list(parse(lines))


def endless_lines() -> Iterator[str]:
    """One INFO line every 20 seconds, forever."""
    start = datetime(2026, 11, 14, 10, 0, tzinfo=UTC)
    for n in count():
        yield f"{(start + timedelta(seconds=20 * n)).isoformat()} INFO svc{n % 3} message {n}"


class CountingSource:
    def __init__(self, lines):
        self.lines = iter(lines)
        self.pulled = 0

    def __iter__(self):
        return self

    def __next__(self):
        line = next(self.lines)
        self.pulled += 1
        return line


def test_parse_builds_entries():
    first = sample_entries()[0]
    assert first == Entry(datetime(2026, 11, 14, 10, 0, 1, tzinfo=UTC), "INFO", "payments", "charge accepted")
    assert sample_entries()[3].message == "slow query (1.2 s)"


def test_parse_skips_malformed_lines():
    lines = [
        "2026-11-14T10:00:00+00:00 INFO",            # no service
        "2026-11-14T10:00:00+00:00 LOUD svc hello",  # unknown level
        "yesterday INFO svc hello",                   # bad time
        "   ",
        "# comment",
        "2026-11-14T10:00:00+00:00 INFO svc",         # no message is fine
    ]
    entries = list(parse(lines))
    assert entries == [Entry(datetime(2026, 11, 14, 10, 0, tzinfo=UTC), "INFO", "svc", "")]


def test_parse_is_lazy():
    source = CountingSource(["# header", "", "2026-11-14T10:00:00+00:00 INFO a x", "2026-11-14T10:00:01+00:00 INFO b y"])
    entries = parse(source)
    assert source.pulled == 0, "nothing is read before the first entry is requested"
    assert next(entries).service == "a"
    assert source.pulled == 3


def test_parse_works_on_an_endless_stream():
    first = list(islice(parse(endless_lines()), 4))
    assert [entry.service for entry in first] == ["svc0", "svc1", "svc2", "svc0"]


def test_at_least_filters_by_level():
    levels = [entry.level for entry in at_least(sample_entries(), "WARNING")]
    assert levels == ["ERROR", "WARNING", "CRITICAL"]
    assert len(list(at_least(sample_entries(), "DEBUG"))) == 6


def test_at_least_rejects_unknown_levels_at_the_call():
    with pytest.raises(ValueError):
        at_least(iter([]), "LOUD")


def test_at_least_is_lazy():
    errors = at_least(parse(endless_lines()), "INFO")
    assert next(errors).message == "message 0"


def test_per_minute_counts_consecutive_runs():
    assert list(per_minute(sample_entries())) == [
        ("2026-11-14T10:00", 3),
        ("2026-11-14T10:01", 2),
        ("2026-11-14T10:03", 1),
    ]


def test_per_minute_is_lazy():
    assert list(islice(per_minute(parse(endless_lines())), 2)) == [
        ("2026-11-14T10:00", 3),
        ("2026-11-14T10:01", 3),
    ]


def test_batches():
    chunks = list(batches(sample_entries(), 4))
    assert [len(chunk) for chunk in chunks] == [4, 2]
    assert all(isinstance(chunk, list) for chunk in chunks)
    assert list(batches([], 3)) == []


def test_batches_rejects_bad_sizes_at_the_call():
    with pytest.raises(ValueError):
        batches(iter([]), 0)


def test_batches_is_lazy():
    first = next(batches(parse(endless_lines()), 2))
    assert [entry.message for entry in first] == ["message 0", "message 1"]


def test_busiest_service():
    assert busiest_service(sample_entries()) == "payments"
    assert busiest_service(parse(["2026-11-14T10:00:00+00:00 INFO b x", "2026-11-14T10:00:00+00:00 INFO a x"])) == "a"
    assert busiest_service([]) is None
