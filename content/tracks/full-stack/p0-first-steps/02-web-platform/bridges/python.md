---
from: python
status: reviewed
concept: "Forms and the DOM behind your templates"
from_way: "Django forms render and validate HTML for you, so the browser side stays invisible."
native_way: "Know what the browser does with that HTML: how a form encodes its fields, how the DOM is updated, how fetch reports errors."
sensei_tip: "Django's autoescape and mark_safe map exactly to textContent and innerHTML: treat mark_safe and |safe as security review items."
---

| Python | Browser |
| --- | --- |
| `{{ comment }}` (autoescaped) | `el.textContent = comment` |
| `{{ comment|safe }}` | `el.innerHTML = comment` |
| `requests.get(...).raise_for_status()` | `if (!response.ok) throw ...` with fetch |
| `request.POST["quantity"]` | the form sends `application/x-www-form-urlencoded` |
