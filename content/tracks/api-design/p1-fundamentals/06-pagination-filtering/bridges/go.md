---
from: go
status: draft
concept: "Pagination, filtering and sorting"
from_way: "`LIMIT $1 OFFSET $2` with a `page` query parameter."
native_way: "Keyset SQL with `database/sql` or pgx, a `Cursor` struct encoded with `base64.RawURLEncoding`, and `strconv`-validated limits."
sensei_tip: "`base64.RawURLEncoding` is the unpadded URL-safe alphabet; cursors then need no escaping in query strings."
---

| Go | Collection concept |
| --- | --- |
| `LIMIT ... OFFSET ...` | offset |
| `WHERE (created_at, id) < ($1, $2)` | keyset cursor |
| `base64.RawURLEncoding` | opaque cursor |
| `strconv.Atoi` + bounds check | validated limit |
| `url.Values.Set("cursor", ...)` | next link |
