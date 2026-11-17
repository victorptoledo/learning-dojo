"""Stage 4 contract: Atom feeds and the digest text."""

import asyncio
from datetime import UTC, datetime

from feedkit import FakeFetch, atom, rss, rss_item

from feeds import Item, Report, digest, fetch_all, parse_feed


def entry(title: str, links: str, published: str | None = None, updated: str | None = None) -> str:
    dates = ""
    if published:
        dates += f"<published>{published}</published>"
    if updated:
        dates += f"<updated>{updated}</updated>"
    return f"<entry><title>{title}</title>{links}{dates}<id>x</id></entry>"


def test_atom_entries_are_read():
    text = atom(
        entry("First", '<link href="https://a/1"/>', published="2026-11-16T09:30:00Z", updated="2026-11-17T00:00:00Z"),
        entry("Second", '<link rel="alternate" href="https://a/2"/>', updated="2026-11-15T10:00:00+02:00"),
    )
    items = parse_feed(text)
    assert [(i.title, i.link) for i in items] == [("First", "https://a/1"), ("Second", "https://a/2")]
    assert items[0].published == datetime(2026, 11, 16, 9, 30, tzinfo=UTC)
    assert items[1].published == datetime(2026, 11, 15, 8, 0, tzinfo=UTC)


def test_the_alternate_link_wins_over_others():
    text = atom(entry("E", '<link rel="self" href="https://a/self"/><link rel="alternate" href="https://a/page"/>'))
    assert parse_feed(text)[0].link == "https://a/page"


def test_rss_and_atom_mix_in_one_report():
    fetch = FakeFetch({
        "rss": rss(rss_item("rss item", "https://r/1", "Sun, 15 Nov 2026 08:00:00 +0000")),
        "atom": atom(entry("atom entry", '<link href="https://a/1"/>', published="2026-11-16T08:00:00Z")),
    })
    report = asyncio.run(fetch_all(["rss", "atom"], fetch))
    assert [i.title for i in report.items] == ["atom entry", "rss item"]


def item(title: str, day: int | None) -> Item:
    published = datetime(2026, 11, day, 12, 0, tzinfo=UTC) if day else None
    return Item(title=title, link=f"https://x/{title}", published=published)


def make_report(items: list[Item], errors: dict[str, str]) -> Report:
    report = asyncio.run(fetch_all([], FakeFetch({})))
    report.items = items
    report.errors = errors
    return report


def test_the_digest_lists_items_and_a_summary():
    report = make_report([item("new", 16), item("older", 15), item("loose", None)], {"down": "timeout"})
    assert digest(report).splitlines() == [
        "2026-11-16 new <https://x/new>",
        "2026-11-15 older <https://x/older>",
        "undated loose <https://x/loose>",
        "3 items, 1 feed failed",
    ]


def test_since_keeps_recent_dated_items():
    report = make_report([item("new", 16), item("older", 15), item("loose", None)], {})
    assert digest(report, since=datetime(2026, 11, 16, tzinfo=UTC)).splitlines() == [
        "2026-11-16 new <https://x/new>",
        "1 item, 0 feeds failed",
    ]


def test_limit_cuts_the_list():
    report = make_report([item(f"i{d}", d) for d in (20, 19, 18, 17)], {"a": "x", "b": "y"})
    lines = digest(report, limit=2).splitlines()
    assert lines == ["2026-11-20 i20 <https://x/i20>", "2026-11-19 i19 <https://x/i19>", "2 items, 2 feeds failed"]


def test_dates_are_shown_in_utc():
    late = Item("late", "https://x/late", datetime.fromisoformat("2026-11-16T23:30:00-03:00"))
    assert digest(make_report([late], {})).splitlines()[0] == "2026-11-17 late <https://x/late>"
