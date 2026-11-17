"""Stage 2 contract: fetch_all fetches concurrently within a limit and keeps failures apart."""

import asyncio
import time

from feedkit import FakeFetch, rss, rss_item

from feeds import fetch_all


def feed(name: str, *dates: str) -> str:
    return rss(*(rss_item(f"{name}{i}", f"https://{name}/{i}", date) for i, date in enumerate(dates)))


def test_items_of_every_feed_newest_first():
    fetch = FakeFetch({
        "a": feed("a", "Mon, 16 Nov 2026 08:00:00 +0000", "Sat, 14 Nov 2026 08:00:00 +0000"),
        "b": feed("b", "Sun, 15 Nov 2026 08:00:00 +0000"),
    })
    report = asyncio.run(fetch_all(["a", "b"], fetch))
    assert [i.title for i in report.items] == ["a0", "b0", "a1"]
    assert report.errors == {}


def test_undated_items_come_last_in_feed_order():
    fetch = FakeFetch({
        "a": rss(rss_item("a-undated", "https://a/u")),
        "b": rss(rss_item("b-dated", "https://b/d", "Mon, 16 Nov 2026 08:00:00 +0000"), rss_item("b-undated", "https://b/u")),
    })
    report = asyncio.run(fetch_all(["a", "b"], fetch))
    assert [i.title for i in report.items] == ["b-dated", "a-undated", "b-undated"]


def test_a_failing_feed_does_not_sink_the_others():
    fetch = FakeFetch({
        "good": feed("g", "Mon, 16 Nov 2026 08:00:00 +0000"),
        "down": ConnectionError("connection refused"),
        "broken": "<html>not a feed</html>",
    })
    report = asyncio.run(fetch_all(["down", "good", "broken"], fetch))
    assert [i.title for i in report.items] == ["g0"]
    assert set(report.errors) == {"down", "broken"}
    assert "connection refused" in report.errors["down"]


def test_duplicate_links_keep_the_first_feeds_item():
    shared = "https://news/1"
    fetch = FakeFetch({
        "first": rss(rss_item("from first", shared, "Mon, 16 Nov 2026 08:00:00 +0000")),
        "second": rss(rss_item("from second", shared, "Mon, 16 Nov 2026 09:00:00 +0000")),
    })
    report = asyncio.run(fetch_all(["first", "second"], fetch))
    assert [i.title for i in report.items] == ["from first"]


def test_a_url_listed_twice_is_fetched_once():
    fetch = FakeFetch({"a": feed("a", "Mon, 16 Nov 2026 08:00:00 +0000")})
    report = asyncio.run(fetch_all(["a", "a"], fetch))
    assert fetch.calls == {"a": 1}
    assert len(report.items) == 1


def test_feeds_are_fetched_concurrently_within_the_limit():
    urls = [f"u{i}" for i in range(10)]
    fetch = FakeFetch({url: feed(url, "Mon, 16 Nov 2026 08:00:00 +0000") for url in urls}, {url: 0.1 for url in urls})
    started = time.perf_counter()
    report = asyncio.run(fetch_all(urls, fetch, limit=5))
    elapsed = time.perf_counter() - started
    assert len(report.items) == 10
    assert fetch.most_at_once == 5, "five at a time, not more and not fewer"
    assert elapsed < 0.6, f"two rounds of 0.1 s took {elapsed:.2f} s: are the fetches concurrent?"


def test_a_limit_of_one_is_sequential():
    urls = ["a", "b", "c"]
    fetch = FakeFetch({url: feed(url, "Mon, 16 Nov 2026 08:00:00 +0000") for url in urls}, {url: 0.01 for url in urls})
    asyncio.run(fetch_all(urls, fetch, limit=1))
    assert fetch.most_at_once == 1
