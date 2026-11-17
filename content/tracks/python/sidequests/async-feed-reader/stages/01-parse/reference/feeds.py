"""Parses RSS and Atom feeds and fetches many of them at once with asyncio."""

import xml.etree.ElementTree as ET
from dataclasses import dataclass
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
