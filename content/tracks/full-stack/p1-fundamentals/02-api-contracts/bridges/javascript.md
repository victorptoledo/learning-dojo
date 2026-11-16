---
from: javascript
status: reviewed
concept: "Types that also check at runtime"
from_way: "JSON.parse and hope, or TypeScript interfaces that give a false sense of safety for data from the network."
native_way: "A zod (or Valibot) schema validates the data and infers the type, and the same package is imported by the server and the client."
sensei_tip: "Type everything that comes from the network as unknown until a schema has parsed it; the compiler then forces you to validate."
---

| Plain JS / TS | With a contract |
| --- | --- |
| `const todo: Todo = await res.json()` | `const todo = Todo.parse(await res.json())` |
| interfaces copied by hand | `type Todo = z.infer<typeof Todo>` |
| `{ error: "bad" }` | `application/problem+json` with `errors` |
| renaming a field freely | expand and contract |
