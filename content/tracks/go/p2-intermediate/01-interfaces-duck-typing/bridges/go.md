---
from: go
status: draft
concept: "Interfaces before and after generics"
from_way: "interface{} everywhere, containers of interface{} with type assertions on the way out, sort.Interface implementations for every slice type, ioutil helpers, and producer-side interfaces copied from Java code bases."
native_way: "any as the spelling of interface{}, generics for containers and algorithms, slices.SortFunc instead of sort.Interface, io and os instead of ioutil, and small consumer-side interfaces."
sensei_tip: "If a function takes interface{} and immediately type-switches on two or three types, it probably wants a type parameter with a constraint instead."
---

| Older Go | Go 1.27 |
| --- | --- |
| `interface{}` | `any` (1.18) |
| `type byAge []Person` + `Len`, `Less`, `Swap` | `slices.SortFunc(people, func(a, b Person) int { ... })` |
| `[]interface{}` container with assertions | `Stack[T any]` |
| `ioutil.ReadAll`, `ioutil.ReadFile` | `io.ReadAll`, `os.ReadFile` (1.16) |
| `func Contains(xs []interface{}, x interface{})` | `slices.Contains(xs, x)` |
| `reflect` to walk struct fields | `reflect.Type.Fields()` iterator (1.26), or generics when types are known |
