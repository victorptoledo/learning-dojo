---
from: python
status: draft
concept: "Rebinding instead of mutating"
from_way: "You mutate lists and dicts in place (append, update) and the same object is seen everywhere."
native_way: "State is replaced, never mutated: spread a copy, change it, and pass the new value to the setter so React notices the new reference."
sensei_tip: "Write React state updates the way you would write code over tuples and frozen dataclasses: dataclasses.replace(user, email=x) is { ...user, email: x }."
---

| Python | React |
| --- | --- |
| `self.count += 1` | `setCount((c) => c + 1)` |
| `items.append(x)` | `setItems([...items, x])` |
| `user["email"] = x` | `setUser({ ...user, email: x })` |
| `dataclasses.replace(user, email=x)` | `{ ...user, email: x }` |
| `[t for t in todos if t.id != id]` | `todos.filter((t) => t.id !== id)` |
| `@property` computed attribute | A value derived during render |
