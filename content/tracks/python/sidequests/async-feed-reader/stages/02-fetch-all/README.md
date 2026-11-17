```python
@dataclass
class Report:
    items: list[Item]
    errors: dict[str, str]   # url -> what went wrong

async def fetch_all(urls: list[str], fetch, *, limit: int = 5) -> Report: ...
```

- Every URL is fetched with `await fetch(url)` and parsed with `parse_feed`, **concurrently**,
  but never more than `limit` fetches at the same time.
- A URL whose fetch raises, or whose text is not a feed, goes to `errors` with a message (the
  exception's text is fine); the other feeds carry on. A URL listed twice is fetched once.
- `items` holds the items of every feed that worked, newest `published` first; items without
  a date come last, in the order their feeds were listed. When two items share a link, only
  the first one (in the order of `urls`) is kept.
