---
from: python
status: draft
concept: "Markup as expressions, not template strings"
from_way: "Jinja templates are strings with {{ }} and {% %} blocks, rendered by the template engine; macros reuse pieces of markup."
native_way: "JSX is real code: tags are function calls, braces hold Python-like expressions, and reusable pieces are components with typed props."
sensei_tip: "A component is a Jinja macro that is a real function: it takes keyword-like props, returns markup, and the type checker sees every call."
---

| Python / Jinja | React |
| --- | --- |
| `{% macro avatar(name, size=48) %}` | `function Avatar({ name, size = 48 }: AvatarProps)` |
| `{{ name }}` (auto-escaped) | `{name}` (escaped) |
| `{% if ok %}...{% endif %}` | `{ok && <X />}` or a ternary |
| `{% for x in xs %}` | `{xs.map((x) => <li key={x}>{x}</li>)}` |
| `{% call card() %}...{% endcall %}` | `<Card>...</Card>` with children |
| `\|safe` filter | `dangerouslySetInnerHTML` |
