---
from: javascript
status: draft
concept: "Testing data, not only code"
from_way: "zod schemas at API boundaries and Jest tests for functions."
native_way: "Batch checks on whole datasets, with failing rows reported and errors blocking publication."
sensei_tip: "A zod schema on each event is a great row-level contract; add batch checks downstream, because no single event can tell you that a whole day of events is missing."
---

| JavaScript / TypeScript | Data quality |
| --- | --- |
| z.object({...}).parse(event) | row-level validity |
| new Set(ids).size === ids.length | a unique check |
| Jest / node:test | unit tests of transformations |
| a shared schema package | a data contract |
| throw to stop a job | a quality gate |
