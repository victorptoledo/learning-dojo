import asyncio
import time

import pytest

from fetcher import Outcome, fetch_all, fetch_strict, first_response


class FakeWeb:
    """An async fetch function with delays and failures per URL, that records what happened."""

    def __init__(self, delays=None, errors=None, default_delay=0.02):
        self.delays = delays or {}
        self.errors = errors or {}
        self.default_delay = default_delay
        self.active = 0
        self.max_active = 0
        self.calls = []
        self.cancelled = []

    async def __call__(self, url: str) -> str:
        self.calls.append(url)
        self.active += 1
        self.max_active = max(self.max_active, self.active)
        try:
            await asyncio.sleep(self.delays.get(url, self.default_delay))
            if url in self.errors:
                raise self.errors[url]
            return f"body of {url}"
        except asyncio.CancelledError:
            self.cancelled.append(url)
            raise
        finally:
            self.active -= 1


def urls(n):
    return [f"https://site/{i}" for i in range(n)]


def test_fetch_all_keeps_the_input_order():
    web = FakeWeb(delays={"https://site/0": 0.05, "https://site/2": 0.0})
    outcomes = asyncio.run(fetch_all(urls(3), web))
    assert [o.url for o in outcomes] == urls(3)
    assert all(o.ok and o.body == f"body of {o.url}" for o in outcomes)


def test_fetch_all_runs_concurrently_up_to_the_limit():
    web = FakeWeb()
    asyncio.run(fetch_all(urls(12), web, limit=4))
    assert web.max_active == 4


def test_fetch_all_default_limit_is_five():
    web = FakeWeb()
    asyncio.run(fetch_all(urls(9), web))
    assert web.max_active == 5


def test_fetch_all_reports_timeouts_without_waiting_for_them():
    web = FakeWeb(delays={"https://site/1": 5.0})
    start = time.perf_counter()
    outcomes = asyncio.run(fetch_all(urls(3), web, timeout=0.1))
    assert time.perf_counter() - start < 2.0
    assert outcomes[1] == Outcome("https://site/1", error="timeout")
    assert outcomes[0].ok and outcomes[2].ok


def test_fetch_all_reports_errors_by_type_and_keeps_going():
    web = FakeWeb(errors={"https://site/0": ConnectionError("refused"), "https://site/2": KeyError("x")})
    outcomes = asyncio.run(fetch_all(urls(4), web))
    assert [o.error for o in outcomes] == ["ConnectionError", None, "KeyError", None]
    assert not outcomes[0].ok and outcomes[0].body is None


def test_fetch_all_fetches_duplicates_once():
    web = FakeWeb()
    outcomes = asyncio.run(fetch_all(["a", "b", "a", "c", "b"], web))
    assert [o.url for o in outcomes] == ["a", "b", "c"]
    assert sorted(web.calls) == ["a", "b", "c"]


def test_fetch_all_rejects_a_bad_limit():
    with pytest.raises(ValueError):
        asyncio.run(fetch_all(["a"], FakeWeb(), limit=0))


def test_fetch_all_with_no_urls():
    assert asyncio.run(fetch_all([], FakeWeb())) == []


def test_fetch_strict_returns_every_body():
    web = FakeWeb()
    assert asyncio.run(fetch_strict(["a", "b", "a"], web)) == {"a": "body of a", "b": "body of b"}
    assert web.max_active == 2


def test_fetch_strict_cancels_the_others_and_raises_a_group():
    web = FakeWeb(delays={"slow-1": 5.0, "slow-2": 5.0, "bad": 0.01}, errors={"bad": ConnectionError("down")})
    start = time.perf_counter()
    with pytest.raises(ExceptionGroup) as caught:
        asyncio.run(fetch_strict(["slow-1", "bad", "slow-2"], web))
    assert time.perf_counter() - start < 2.0
    assert caught.group_contains(ConnectionError)
    assert sorted(web.cancelled) == ["slow-1", "slow-2"]


def test_first_response_returns_the_fastest_success_and_cancels_the_rest():
    web = FakeWeb(delays={"slow": 5.0, "fast": 0.02, "broken": 0.0}, errors={"broken": OSError()})
    start = time.perf_counter()
    assert asyncio.run(first_response(["slow", "broken", "fast"], web)) == ("fast", "body of fast")
    assert time.perf_counter() - start < 2.0
    assert web.cancelled == ["slow"]
    assert web.active == 0


def test_first_response_when_everything_fails():
    web = FakeWeb(errors={"a": OSError(), "b": ValueError()})
    with pytest.raises(LookupError):
        asyncio.run(first_response(["a", "b"], web))
    with pytest.raises(LookupError):
        asyncio.run(first_response([], web))


def test_first_response_times_out():
    web = FakeWeb(default_delay=5.0)
    start = time.perf_counter()
    with pytest.raises(TimeoutError):
        asyncio.run(first_response(["a", "b"], web, timeout=0.1))
    assert time.perf_counter() - start < 2.0
    assert sorted(web.cancelled) == ["a", "b"]
