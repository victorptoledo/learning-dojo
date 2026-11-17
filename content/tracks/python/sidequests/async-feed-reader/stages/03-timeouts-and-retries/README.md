A site that never answers must not keep the reader waiting forever.

```python
async def fetch_all(urls, fetch, *, limit=5, timeout=None, retries=0, retry_delay=0.0) -> Report
```

- `timeout` (seconds): each attempt to fetch a URL gives up after that long; a URL that times
  out on every attempt has the error `timeout`.
- `retries`: a fetch that raises or times out is tried again, up to `retries` more times,
  waiting `retry_delay` seconds before each new try. A feed that downloads but does not parse
  is **not** retried (trying again would give the same text).
- The report gains `attempts: dict[str, int]`, how many times each URL was fetched.

Timeouts and retries are per URL: a retrying URL never stops the others from finishing, and
it still counts towards `limit` while it waits.
