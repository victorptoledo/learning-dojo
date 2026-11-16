---
from: python
status: reviewed
concept: "From a Django or FastAPI view to the whole journey"
from_way: "A Python web developer writes views and lets someone else worry about the browser and the infrastructure."
native_way: "Follow each click from the page through nginx or a CDN to the view, the ORM's SQL and back to what the user sees."
sensei_tip: "Django's debug toolbar or SQLAlchemy echo plus the browser's network tab show you both ends of the same request."
---

| Python habit | End to end |
| --- | --- |
| `def view(request)` is the start | DNS, TLS, a proxy and a load balancer come first |
| `print` or `logging` | structured logs with a trace id the UI can show |
| `manage.py migrate` on the laptop | migrations as a release step |
| templates in Jinja or Django | the page, its JavaScript and its states are yours too |
