---
from: python
status: draft
concept: "Comprehensions that return elements"
from_way: "Jinja loops over lists with for blocks; desktop toolkits bind callbacks with command=... or connect()."
native_way: "Lists are rendered with map, which is a list comprehension returning elements; handlers are functions passed in props."
sensei_tip: "items.map((x) => <li key={x.id}>{x.name}</li>) is [f'<li>{x.name}</li>' for x in items] with an identity attached: always pick the id as key."
---

| Python | React |
| --- | --- |
| `Button(command=save)` (Tkinter) | `<button onClick={save}>` |
| `lambda: remove(item_id)` | `() => remove(item.id)` |
| `[x for x in xs if not x.done]` | `xs.filter((x) => !x.done)` |
| `{% for x in xs %}<li>` | `xs.map((x) => <li key={x.id}>...)` |
| `x if cond else y` | `cond ? x : y` |
| `0 and x` is `0` | `0 && x` renders `0` |
