# Async Fetch

Write three coroutines in `fetcher.py` that fetch many URLs concurrently with `asyncio`. The
network is **injected**: `fetch` is any async function `fetch(url) -> str` that returns the
body or raises (the tests pass a fake with delays and failures, and watch how many calls run at
once and which ones get cancelled). `Outcome(url, body=None, error=None)` is already written;
its `ok` property is true when there is no error.

In all three, a URL that appears more than once is fetched **once**, at its first position.

## `fetch_all(urls, fetch, *, limit=5, timeout=1.0) -> list[Outcome]`

Fetch every URL concurrently, with **at most `limit` fetches running at the same time**, and
return one `Outcome` per URL **in input order**:

| Result of the fetch | Outcome |
| --- | --- |
| returned `body` | `Outcome(url, body=body)` |
| took longer than `timeout` seconds | `Outcome(url, error="timeout")`, without waiting for it to finish |
| raised any other exception | `Outcome(url, error=<the exception's class name>)`, e.g. `"ConnectionError"` |

One failure never affects the others. A `limit` below 1 raises `ValueError`.

## `fetch_strict(urls, fetch) -> dict[str, str]`

All or nothing: fetch everything concurrently (no limit) and return `{url: body}` in input
order. If any fetch fails, **cancel the ones still running** and raise the failures as an
`ExceptionGroup`. (An `asyncio.TaskGroup` does exactly this.)

## `first_response(urls, fetch, *, timeout=1.0) -> tuple[str, str]`

Start every fetch at once and return `(url, body)` of the **first one to succeed**; failures
are skipped. Before returning, **cancel the fetches still running** (and wait for them to
finish cancelling). If every fetch fails, or there are no URLs, raise `LookupError`. If nothing
succeeds within `timeout` seconds, raise `TimeoutError`, also after cancelling everything.

Tools: `asyncio.Semaphore`, `asyncio.timeout`, `asyncio.TaskGroup`, `asyncio.create_task`,
`asyncio.wait(..., return_when=asyncio.FIRST_COMPLETED)`, `task.cancel()`,
`asyncio.gather(..., return_exceptions=True)`, `dict.fromkeys`.

The tests run each coroutine with `asyncio.run(...)`. Run the visible tests with
`uv run pytest`. There are 14 tests: 1 in `tests/test_visible.py` and 13 hidden ones, plus ruff
on `fetcher.py`.
