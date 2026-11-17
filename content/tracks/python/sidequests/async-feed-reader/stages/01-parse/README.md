```python
@dataclass(frozen=True)
class Item:
    title: str
    link: str
    published: datetime | None

class FeedError(Exception): ...

def parse_feed(text: str) -> list[Item]: ...
```

`parse_feed` reads an RSS 2.0 document (`<rss><channel><item>...`) and returns its items in
document order:

- `title` and `link` are the text of the item's `<title>` and `<link>`, with surrounding
  spaces removed; a missing title is `""`. An item without a link is skipped.
- `published` is the item's `<pubDate>` (RFC 822, like `Mon, 16 Nov 2026 09:30:00 +0000`) as
  a timezone-aware `datetime`, or `None` when it is missing or cannot be read.
- Text that is not XML, or XML that is not an RSS feed, raises `FeedError`.
