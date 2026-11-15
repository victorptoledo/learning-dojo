---
from: javascript
status: draft
concept: "Client-server communication"
from_way: "fetch and Express, with headers and status codes often left at their defaults."
native_way: "Methods, status codes and headers chosen for their meaning."
sensei_tip: "Express sets ETags on res.send by default (weak ones). Know what they do before relying on them, and send Cache-Control explicitly."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| res.json(...) | 200 with Content-Type application/json |
| res.status(201).location(url) | 201 Created |
| app.set('etag', ...) | ETag and If-None-Match |
| fetch(url, { cache }) | client and proxy caching rules |
