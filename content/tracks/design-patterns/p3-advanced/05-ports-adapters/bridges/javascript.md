---
from: javascript
status: draft
concept: "Ports and adapters"
from_way: "Express handlers with business logic, SQL and vendor calls in the same function."
native_way: "A domain folder with plain functions and objects that take their ports as parameters, adapters for HTTP, the database and vendors, and an entry file that wires them; dependency-cruiser to enforce the rule."
sensei_tip: "dependency-cruiser (or eslint-plugin-boundaries) can fail the build when `src/domain` imports from `src/adapters` or from a vendor SDK."
---

| JavaScript | Ports and adapters |
| --- | --- |
| a `rates` object passed into the domain | driven port |
| Express or Fastify routes | driving adapters |
| a module wrapping the vendor SDK | driven adapter, ACL |
| `index.js` wiring | composition root |
| dependency-cruiser rules | enforcing the dependency rule |
