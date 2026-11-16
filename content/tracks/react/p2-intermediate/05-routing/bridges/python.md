---
from: python
status: draft
concept: "urls.py in the browser"
from_way: "Django's urls.py maps path('products/<int:id>/', view); templates extend a base layout; request.GET holds query parameters."
native_way: "React Router maps paths to components on the client; nested routes render inside an Outlet; useSearchParams reads the query string."
sensei_tip: "Write the route tree like urls.py with include(): the parent path and its layout, then the children. The difference is that nothing reloads when the user moves between them."
---

| Django | React Router |
| --- | --- |
| `path("products/<int:pk>/", view)` | `{ path: "products/:productId", element: ... }` |
| `include("account.urls")` | `children: [...]` of the account route |
| `{% extends "base.html" %}` + `{% block %}` | Layout route + `<Outlet />` |
| `request.GET["page"]` | `searchParams.get("page")` |
| `redirect("login")` | `redirect("/login")` or `<Navigate>` |
| `@login_required` | A guard in the loader or a wrapper route |
