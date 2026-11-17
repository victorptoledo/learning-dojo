"""Parses RSS and Atom feeds and fetches many of them at once with asyncio."""

import asyncio
from collections.abc import Awaitable, Callable
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from dataclasses import field
from datetime import UTC, datetime
from email.utils import parsedate_to_datetime

ATOM = "{http://www.w3.org/2005/Atom}"

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

def _iso_date(text: str) -> datetime | None:
    try:
        parsed = datetime.fromisoformat(text)
    except ValueError:
        return None
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=UTC)

def _atom_items(root: ET.Element) -> list[Item]:
    items = []
    for entry in root.findall(f"{ATOM}entry"):
        link = next(
            (node.get("href", "").strip() for node in entry.findall(f"{ATOM}link")
             if node.get("rel", "alternate") == "alternate"),
            "",
        )
        if not link:
            continue
        date = _text(entry.find(f"{ATOM}published")) or _text(entry.find(f"{ATOM}updated"))
        items.append(Item(_text(entry.find(f"{ATOM}title")), link, _iso_date(date) if date else None))
    return items

def parse_feed(text: str) -> list[Item]:
    try:
        root = ET.fromstring(text)
    except ET.ParseError as error:
        raise FeedError(f"not XML: {error}") from error
    if root.tag == f"{ATOM}feed":
        return _atom_items(root)
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

def digest(report: Report, *, since: datetime | None = None, limit: int = 10) -> str:
    chosen = [i for i in report.items if since is None or (i.published and i.published >= since)][:limit]
    lines = []
    for item in chosen:
        day = item.published.astimezone(UTC).strftime("%Y-%m-%d") if item.published else "undated"
        lines.append(f"{day} {item.title} <{item.link}>")
    items = "1 item" if len(chosen) == 1 else f"{len(chosen)} items"
    failed = "1 feed failed" if len(report.errors) == 1 else f"{len(report.errors)} feeds failed"
    lines.append(f"{items}, {failed}")
    return "\n".join(lines)
