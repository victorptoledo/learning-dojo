---
from: java
status: draft
concept: "Dependency injection and the command pattern"
from_way: "Spring injects beans into whatever needs them; state transitions are often methods on a service, or commands handled by a dispatcher."
native_way: "Context injects a value into a subtree; a reducer is a pure function that handles command-like actions and returns the next state."
sensei_tip: "Context is constructor injection for components, without the constructor. A reducer is a sealed interface of commands plus one switch that returns a new immutable state."
---

| Java | React |
| --- | --- |
| `@Autowired SessionService` | `const session = useSession()` |
| Bean scope | The provider's position in the tree |
| `sealed interface CartCommand` | `type CartAction = { type: "add" } \| ...` |
| `switch (cmd) { case Add a -> ... }` | `switch (action.type) { case "add": ... }` |
| Immutable state record returned | Reducer returns a new state object |
| Missing bean: startup error | Missing provider: the guard hook throws |
