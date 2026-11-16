---
from: python
status: draft
concept: "Components and architecture"
from_way: "Django templates with includes and template tags; htmx for server-driven interactivity."
native_way: "Custom elements add reusable, encapsulated interactive parts on top of server-rendered HTML: islands of interactivity."
sensei_tip: "htmx and web components fit Django well: the server renders HTML, small elements enhance it; that is progressive enhancement."
---

| Python | In the browser |
| --- | --- |
| `{% include "card.html" %}` | A custom element with a template |
| A template tag | A custom element's behavior |
| htmx partial updates | Islands enhanced by small scripts |
| Django's static pages with caching | Static site generation |
