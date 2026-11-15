---
from: javascript
status: draft
concept: "From RPC endpoints to resources"
from_way: "RPC-style Express routes and ad hoc JSON shapes."
native_way: "Resources, consistent pagination and a standard error format."
sensei_tip: "Design the URLs as nouns and use the method as the verb. JSON Merge Patch (application/merge-patch+json) is the simplest partial update for JSON clients."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| POST /updateUser | PATCH /users/{id} |
| res.status(400).json({ msg }) | Problem Details |
| offset/limit without bounds | bounded limits and cursors |
| breaking a field name | additive changes only |
