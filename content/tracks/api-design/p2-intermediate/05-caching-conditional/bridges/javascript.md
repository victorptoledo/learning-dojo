---
from: javascript
status: draft
concept: "HTTP caching and conditional requests"
from_way: "Express's automatic weak ETags on every `res.send` and no idea where they come from."
native_way: "Explicit `res.set('ETag', ...)`, `req.fresh` for 304s, and an `If-Match` check against a version column before updates."
sensei_tip: "Express generates weak ETags (`W/\"...\"`) by default; they are fine for `If-None-Match` but never match `If-Match`, which needs strong tags."
---

| JavaScript / TypeScript | HTTP caching |
| --- | --- |
| `res.set('ETag', '"7"')` | strong ETag |
| `req.fresh` | 304 check |
| `app.set('etag', false)` | turn off automatic ETags |
| `res.set('Cache-Control', 'private, no-cache')` | revalidate |
| `req.get('If-Match')` | conditional write |
