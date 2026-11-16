---
from: go
status: draft
concept: "Bounded contexts and context maps"
from_way: "One `models` package with a `Customer` struct used by every service, and a vendor SDK's structs passed through business code."
native_way: "A package tree per bounded context with its own structs, a translating adapter package that converts the vendor SDK's types into them, and ids passed between contexts."
sensei_tip: "Go has no inheritance to tempt you into one `Customer` for everyone. Two small structs with the same id in two packages is idiomatic, and so is a function that converts between them at the edge."
---

| Go | Strategic DDD idea |
| --- | --- |
| a package tree per context | bounded context |
| structs named in the experts' terms | ubiquitous language in code |
| an adapter package over a vendor SDK | anti-corruption layer |
| a protobuf API you publish | open host service with published language |
| a tiny shared package (money, ids) | shared kernel |
| `depguard`: only the adapter imports the SDK | the ACL enforced |
