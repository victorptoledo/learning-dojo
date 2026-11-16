---
from: python
status: draft
concept: "Bounded contexts and context maps"
from_way: "One `models.py` with a `Customer` model shared by every Django app, and a partner's JSON used as dicts all over the code."
native_way: "A package per bounded context with its own dataclasses, a translation module that turns the partner's JSON into them (an ACL), and ids passed between contexts."
sensei_tip: "A raw `dict` from another system flowing through your code is the purest form of conformism. Parse it once at the boundary, with Pydantic or a dataclass constructor, into your context's types."
---

| Python | Strategic DDD idea |
| --- | --- |
| a package or Django app per context | bounded context |
| dataclasses named in the experts' terms | ubiquitous language in code |
| a translation module with Pydantic models | anti-corruption layer |
| a FastAPI service with a documented schema | open host service with published language |
| a small shared package (money, ids) | shared kernel |
| `import-linter`: only `acl` imports the client | the ACL enforced |
