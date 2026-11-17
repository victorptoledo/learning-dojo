Lists grow; clients want pages.

- `GET /tasks` takes `?limit=` (1 to 100, 20 by default) and `?offset=` (0 or more, 0 by
  default): `items` holds that page, `total` still counts every matching task, and a new field
  `next_offset` is the offset of the next page, or `null` on the last one.
- `?sort=` orders the list: `id` (the default), `-id`, `title` or `-title` (titles compare
  ignoring case; equal titles go by id in the same direction, so `-title` is exactly `title`
  reversed). Anything else is `422`.
- Filters, sorting and paging combine, in SQL (`ORDER BY`, `LIMIT`, `OFFSET`).

```json
{"items": [{"id": 3, "title": "c", "done": false}], "total": 5, "next_offset": 3}
```
