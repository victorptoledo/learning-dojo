---
from: java
status: draft
concept: "Immutable values and a single owner"
from_way: "Objects hold mutable fields changed with setters, and observers or bindings (JavaFX properties) propagate changes to the UI."
native_way: "State is an immutable snapshot owned by one component; you replace it with a new value through the setter, and React re-renders."
sensei_tip: "Think of state as a record you never mutate: to change one field, build a new record with the change (like a wither) and hand it to the setter."
---

| Java | React |
| --- | --- |
| `private int count;` field | `const [count, setCount] = useState(0)` |
| `this.count++` | `setCount((c) => c + 1)` |
| `user.setEmail(x)` | `setUser({ ...user, email: x })` |
| `list.add(x)` | `setList([...list, x])` |
| `List.copyOf`, records | Spread copies, readonly types |
| Observer pattern to notify the view | Setting state re-renders automatically |
