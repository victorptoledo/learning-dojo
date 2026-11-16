---
from: python
status: draft
concept: "Encode for the context the data lands in"
from_way: "Building HTML with f-strings, or `|safe` and `Markup()` in Jinja2 templates."
native_way: "Templates with autoescape on, html.escape for hand-built fragments, nh3 for user HTML, and re.fullmatch for validation."
sensei_tip: "Flask enables Jinja2 autoescaping for .html templates only; a template named .txt or rendered with `render_template_string` from a variable may not be escaped the way you expect."
---

| Python | XSS-safe way |
| --- | --- |
| f"<p>{name}</p>" | html.escape(name) or a template |
| {{ bio }} marked safe | nh3.clean(bio) then render |
| re.match(r"^...$", s) | re.fullmatch(r"...", s) |
| json.dumps inside <script> | json_script filter or escaped JSON |
| javascript: links | urlsplit and an http/https allow-list |
