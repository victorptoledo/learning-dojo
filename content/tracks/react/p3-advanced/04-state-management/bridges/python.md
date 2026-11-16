---
from: python
status: draft
concept: "Where state lives in a web app"
from_way: "In Django, data lives in the database, the session holds the user, the URL holds filters, and caches sit in front of slow queries."
native_way: "In React, server data lives in a query cache, the user in context or a store, filters in the URL, and everything else as local component state."
sensei_tip: "Ask the Django question: is this data owned by the server? Then it is a cache on the client. Only what the browser truly owns is client state."
---

| Django | React |
| --- | --- |
| Database rows | Server state in a query cache |
| `request.session["user"]` | Context or a store |
| `request.GET["page"]` | Search params |
| `django.core.cache` | TanStack Query cache |
| View-local variables | Local component state |
| Signals | Store subscriptions |
