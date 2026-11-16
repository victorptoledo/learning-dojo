---
from: javascript
status: draft
concept: "Bounded contexts and context maps"
from_way: "One `types.ts` with a `Customer` interface used by every part of the app, and third-party API responses passed around as they come."
native_way: "A folder or package per bounded context with its own types, a translation module that validates and maps external responses (with Zod) into them, and ids shared between contexts."
sensei_tip: "Validate and map external data at the boundary with a schema library such as Zod: the parsed type is your context's model, and the raw response never travels further."
---

| JavaScript / TypeScript | Strategic DDD idea |
| --- | --- |
| a workspace package per context | bounded context |
| types named in the experts' terms | ubiquitous language in code |
| a mapper with Zod schemas over an external API | anti-corruption layer |
| an OpenAPI or GraphQL schema you publish | open host service with published language |
| a tiny shared package (money, ids) | shared kernel |
| `dependency-cruiser`: only `acl` imports the SDK | the ACL enforced |
