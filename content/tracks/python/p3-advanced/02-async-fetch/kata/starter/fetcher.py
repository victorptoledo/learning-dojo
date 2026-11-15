"""Concurrent fetching with asyncio: limits, timeouts, task groups and the first answer."""

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
    raise NotImplementedError("todo: write fetch_all()")


async def fetch_strict(urls: Iterable[str], fetch: Fetch) -> dict[str, str]:
    """All or nothing: every body, or the failures as an ExceptionGroup."""
    raise NotImplementedError("todo: write fetch_strict()")


async def first_response(urls: Iterable[str], fetch: Fetch, *,
                         timeout: float = 1.0) -> tuple[str, str]:
    """The first (url, body) to succeed; the other fetches are cancelled."""
    raise NotImplementedError("todo: write first_response()")
