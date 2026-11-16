---
from: python
status: draft
concept: "Templates that re-run when data changes"
from_way: "Django or Jinja templates render HTML once per request from a context dict; a Tkinter UI mutates widgets with config()."
native_way: "A React component is a function that renders JSX from props and state, and it re-runs in the browser every time that state changes."
sensei_tip: "Think of a component as a template function that React calls again whenever its data changes, then patches only the parts of the page that differ."
---

| Python | React |
| --- | --- |
| `pip install`, `python manage.py runserver` | `npm install`, `npm run dev` |
| Jinja template with `{{ name }}` | JSX with `{name}` |
| Template rendered once per request | Component re-rendered on every state change |
| `widget.config(text="Saved")` (Tkinter) | `setStatus("saved")` |
| Django is batteries-included | React is the view; frameworks add routing and data |
| `python -X dev` extra checks | StrictMode extra checks in development |
