---
from: python
status: draft
concept: "Django views and htmx, merged into components"
from_way: "Django renders templates on the server with ORM queries, and htmx or a JS framework adds interactivity; views handle POSTs."
native_way: "Server Components query data and render on the server like views; Client Components add interactivity; server functions handle the POSTs."
sensei_tip: "A Server Component is a view that returns components instead of a template; a server function is a POST view, and it needs the same @login_required and form validation."
---

| Django | React frameworks |
| --- | --- |
| View with ORM query + template | async Server Component |
| htmx or JS widgets | Client Components |
| POST view with a Form | Server function with validation |
| `@login_required` | Auth check in the server function |
| Static export (e.g. Wagtail bakery) | SSG |
| Context passed to template | Serialisable props |
