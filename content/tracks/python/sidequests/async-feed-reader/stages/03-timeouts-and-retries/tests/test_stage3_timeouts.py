"""Stage 3 contract: per-attempt timeouts, retries with a delay, and attempt counts."""

import asyncio
import time

from feedkit import FakeFetch, rss, rss_item

from feeds import fetch_all

GOOD = rss(rss_item("ok", "https://ok/1", "Mon, 16 Nov 2026 08:00:00 +0000"))


def test_a_hanging_site_times_out():
    fetch = FakeFetch({"slow": GOOD, "fast": GOOD.replace("ok", "fast")}, {"slow": 10})
    started = time.perf_counter()
    report = asyncio.run(fetch_all(["slow", "fast"], fetch, timeout=0.2))
    assert time.perf_counter() - started < 2
    assert report.errors == {"slow": "timeout"}
    assert [i.title for i in report.items] == ["fast"]


def test_a_failure_is_retried_until_it_works():
    fetch = FakeFetch({"flaky": [ConnectionError("reset"), ConnectionError("reset"), GOOD]})
    report = asyncio.run(fetch_all(["flaky"], fetch, retries=2))
    assert report.errors == {}
    assert report.attempts == {"flaky": 3}
    assert [i.title for i in report.items] == ["ok"]


def test_retries_run_out():
    fetch = FakeFetch({"down": ConnectionError("refused")})
    report = asyncio.run(fetch_all(["down"], fetch, retries=2))
    assert report.attempts == {"down": 3}
    assert "refused" in report.errors["down"]


def test_a_timeout_is_retried_too():
    fetch = FakeFetch({"slow-once": GOOD}, {"slow-once": [10, 0]})
    report = asyncio.run(fetch_all(["slow-once"], fetch, timeout=0.2, retries=1))
    assert report.errors == {}
    assert report.attempts == {"slow-once": 2}


def test_a_feed_that_does_not_parse_is_not_retried():
    fetch = FakeFetch({"html": "<html>no feed</html>"})
    report = asyncio.run(fetch_all(["html"], fetch, retries=3))
    assert report.attempts == {"html": 1}
    assert "html" in report.errors


def test_retries_wait_between_tries():
    fetch = FakeFetch({"flaky": [ConnectionError("x"), GOOD]})
    started = time.perf_counter()
    asyncio.run(fetch_all(["flaky"], fetch, retries=1, retry_delay=0.2))
    assert time.perf_counter() - started >= 0.2


def test_a_retrying_url_does_not_hold_the_others():
    urls = ["flaky"] + [f"u{i}" for i in range(4)]
    answers = {u: GOOD.replace("ok/1", f"{u}/1") for u in urls}
    answers["flaky"] = [ConnectionError("x"), ConnectionError("x"), answers["flaky"]]
    fetch = FakeFetch(answers, {u: 0.05 for u in urls})
    started = time.perf_counter()
    report = asyncio.run(fetch_all(urls, fetch, limit=5, retries=2, retry_delay=0.1))
    assert time.perf_counter() - started < 1.0
    assert len(report.items) == 5
    assert report.attempts["u0"] == 1 and report.attempts["flaky"] == 3
