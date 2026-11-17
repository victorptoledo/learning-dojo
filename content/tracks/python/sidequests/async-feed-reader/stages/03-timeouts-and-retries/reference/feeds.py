"""Parses RSS and Atom feeds and fetches many of them at once with asyncio."""

import asyncio
from collections.abc import Awaitable, Callable
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from dataclasses import field
from datetime import UTC, datetime
from email.utils import parsedate_to_datetime

@dataclass(frozen=True)
class Item:
    title: str
    link: str
    published: datetime | None

class FeedError(Exception):
    """The text is not a feed this module can read."""

def _text(element: ET.Element | None) -> str:
    return (element.text or "").strip() if element is not None else ""

def _rss_date(text: str) -> datetime | None:
    try:
        parsed = parsedate_to_datetime(text)
    except (TypeError, ValueError, IndexError):
        return None
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=UTC)

def parse_feed(text: str) -> list[Item]:
    try:
        root = ET.fromstring(text)
    except ET.ParseError as error:
        raise FeedError(f"not XML: {error}") from error
    channel = root.find("channel")
    if root.tag != "rss" or channel is None:
        raise FeedError(f"not a feed: <{root.tag}>")
    items = []
    for node in channel.findall("item"):
        link = _text(node.find("link"))
        if not link:
            continue
        date = _text(node.find("pubDate"))
        items.append(Item(_text(node.find("title")), link, _rss_date(date) if date else None))
    return items

@dataclass
class Report:
    items: list[Item]
    errors: dict[str, str]
    attempts: dict[str, int] = field(default_factory=dict)

def _merge(results: list[list[Item]]) -> list[Item]:
    """First item per link (in feed order), newest first, undated last in feed order."""
    seen: set[str] = set()
    unique = []
    for items in results:
        for item in items:
            if item.link not in seen:
                seen.add(item.link)
                unique.append(item)
    dated = sorted((i for i in unique if i.published), key=lambda i: i.published, reverse=True)
    return dated + [i for i in unique if not i.published]

async def fetch_all(
    urls: list[str],
    fetch: Callable[[str], Awaitable[str]],
    *,
    limit: int = 5,
    timeout: float | None = None,
    retries: int = 0,
    retry_delay: float = 0.0,
) -> Report:
    unique_urls = list(dict.fromkeys(urls))
    gate = asyncio.Semaphore(limit)
    errors: dict[str, str] = {}
    attempts: dict[str, int] = {}

    async def one(url: str) -> list[Item]:
        async with gate:
            for attempt in range(retries + 1):
                attempts[url] = attempt + 1
                if attempt:
                    await asyncio.sleep(retry_delay)
                try:
                    async with asyncio.timeout(timeout):
                        text = await fetch(url)
                    break
                except TimeoutError:
                    error = "timeout"
                except Exception as failure:  # noqa: BLE001 - any failure of a site is reported
                    error = str(failure) or type(failure).__name__
            else:
                errors[url] = error
                return []
        try:
            return parse_feed(text)
        except FeedError as error:
            errors[url] = f"parse error: {error}"
            return []

    results = await asyncio.gather(*(one(url) for url in unique_urls))
    return Report(_merge(list(results)), errors, attempts)
