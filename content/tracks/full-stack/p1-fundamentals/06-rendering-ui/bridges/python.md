---
from: python
status: reviewed
concept: "Django templates and when to add a SPA"
from_way: "Django templates for everything, or a separate React app talking to DRF for everything."
native_way: "Server pages for content and forms, a client-rendered part where interaction demands it, and the same validation feeding both."
sensei_tip: "Django forms already redisplay values and errors per field; mirror that experience in your SPA with the API's field errors."
---

| Django | Rendering practice |
| --- | --- |
| `render(request, "notes.html", ctx)` | server-rendered page |
| `form.errors` in the template | errors under each field |
| `redirect("notes")` after POST | Post/Redirect/Get |
| `|safe` or `mark_safe` | raw HTML: review it |
