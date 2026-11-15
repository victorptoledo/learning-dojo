---
from: go
status: draft
concept: "Composition, coupling and cohesion"
from_way: "Looking for inheritance and building it with deeply embedded structs that promote every method of every layer."
native_way: "Go has no inheritance. Structs hold other values, small interfaces describe roles, and embedding is used sparingly to reuse a method set."
sensei_tip: "Embedding is still composition, but it promotes all methods of the embedded type into your API. Prefer a named field unless you really want to expose them."
---

| Go | Idea |
| --- | --- |
| a struct field holding an interface | composition |
| struct embedding | method promotion (use sparingly) |
| package-level `var` state | common coupling |
| small consumer-side interfaces | low coupling |
| one package per concept | high cohesion |
