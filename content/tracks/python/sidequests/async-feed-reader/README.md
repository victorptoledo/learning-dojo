# Sidequest: Async feed reader

Build `feeds.py`, the core of a news reader: it parses RSS and Atom feeds and fetches many of
them **concurrently** with `asyncio`, so one slow or broken site never holds up the rest.

The network stays outside: `fetch_all` receives the function that downloads a URL,

```python
async def fetch(url: str) -> str: ...   # returns the feed's XML text, or raises
```

so the contract tests pass a fake one that sleeps, fails or hangs on purpose, and the dojo's
checks run offline. In a real program you would pass one built on `urllib` in a thread, or on
an HTTP library.

What the tests use: `Item` (a dataclass with `title`, `link`, `published`), `FeedError`,
`parse_feed` and `fetch_all`, plus `digest` in the last stage. Parse XML with
`xml.etree.ElementTree` and dates with `email.utils` and `datetime`; standard library only.

Feeds come from strangers. The standard parser does not fetch external entities, but a real
reader should parse with `defusedxml` (not available offline here) and cap the size of what
it downloads: worth asking the AI review about.

New to `asyncio`? You need `async def`/`await`, `asyncio.gather` (or a `TaskGroup`),
`asyncio.Semaphore` and `asyncio.timeout`. The tests call `asyncio.run(...)` themselves.
