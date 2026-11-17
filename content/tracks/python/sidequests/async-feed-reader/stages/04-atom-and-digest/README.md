Half the web publishes Atom, and people want a summary, not a list of objects.

- `parse_feed` also reads Atom 1.0 (`<feed xmlns="http://www.w3.org/2005/Atom">` with
  `<entry>` elements): `title` is the entry's `<title>`, `link` the `href` of its `<link>`
  whose `rel` is `alternate` or missing, `published` its `<published>`, or `<updated>` when
  there is no `<published>` (ISO 8601 such as `2026-11-16T09:30:00Z`).
- A new function writes the digest:

```python
def digest(report: Report, *, since: datetime | None = None, limit: int = 10) -> str: ...
```

  It keeps the items published at or after `since` (with `since`, undated items are left
  out), at most `limit` of them in the report's order, one line each, then a summary line:

```text
2026-11-16 Release notes <https://example.com/notes>
2026-11-15 A quiet week <https://blog.example.com/quiet>
2 items, 1 feed failed
```

  A line is the date (`YYYY-MM-DD` in UTC, or `undated`), the title and the link in angle
  brackets. The summary says `1 item`/`N items` and `1 feed failed`/`N feeds failed`, counting
  the items printed and the URLs in `errors`.
