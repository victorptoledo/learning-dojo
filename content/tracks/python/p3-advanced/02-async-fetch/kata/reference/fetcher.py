"""Concurrent fetching with asyncio: limits, timeouts, task groups and the first answer."""

import asyncio
from collections.abc import Awaitable, Callable, Iterable
from dataclasses import dataclass

type Fetch = Callable[[str], Awaitable[str]]


@dataclass(frozen=True, slots=True)
class Outcome:
    url: str
    body: str | None = None
    error: str | None = None

    @property
    def ok(self) -> bool:
        return self.error is None


async def fetch_all(urls: Iterable[str], fetch: Fetch, *, limit: int = 5,
                    timeout: float = 1.0) -> list[Outcome]:
    """Fetch every URL, at most `limit` at a time, each within `timeout` seconds."""
    if limit < 1:
        raise ValueError("limit must be at least 1")
    unique = list(dict.fromkeys(urls))
    semaphore = asyncio.Semaphore(limit)

    async def one(url: str) -> Outcome:
        async with semaphore:
            try:
                async with asyncio.timeout(timeout):
                    return Outcome(url, body=await fetch(url))
            except TimeoutError:
                return Outcome(url, error="timeout")
            except Exception as error:
                return Outcome(url, error=type(error).__name__)

    async with asyncio.TaskGroup() as group:
        tasks = [group.create_task(one(url)) for url in unique]
    return [task.result() for task in tasks]


async def fetch_strict(urls: Iterable[str], fetch: Fetch) -> dict[str, str]:
    """All or nothing: every body, or the failures as an ExceptionGroup."""
    unique = list(dict.fromkeys(urls))
    async with asyncio.TaskGroup() as group:
        tasks = {url: group.create_task(fetch(url)) for url in unique}
    return {url: task.result() for url, task in tasks.items()}


async def first_response(urls: Iterable[str], fetch: Fetch, *,
                         timeout: float = 1.0) -> tuple[str, str]:
    """The first (url, body) to succeed; the other fetches are cancelled."""
    tasks = {asyncio.create_task(fetch(url)): url for url in dict.fromkeys(urls)}
    if not tasks:
        raise LookupError("no URLs")
    try:
        async with asyncio.timeout(timeout):
            pending = set(tasks)
            while pending:
                done, pending = await asyncio.wait(pending, return_when=asyncio.FIRST_COMPLETED)
                for task in done:
                    if task.exception() is None:
                        return tasks[task], task.result()
    finally:
        for task in tasks:
            task.cancel()
        await asyncio.gather(*tasks, return_exceptions=True)
    raise LookupError("every fetch failed")
