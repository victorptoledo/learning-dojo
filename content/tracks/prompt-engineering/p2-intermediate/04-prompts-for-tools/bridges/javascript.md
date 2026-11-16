---
from: javascript
status: draft
concept: "Route handlers for a model"
from_way: "Express or Next.js route handlers with a validation schema and middleware for authentication."
native_way: "Each tool is a handler with a JSON Schema; the model sends a call, your code validates it, checks permissions, runs it and returns a result or a helpful error."
sensei_tip: "If you already validate request bodies with Zod, use the same schema for the tool: convert it to JSON Schema for the definition and safeParse the model's arguments before running the handler."
---

| JavaScript | Tool-use concept |
| --- | --- |
| a route handler | a tool |
| a Zod schema for the body | the tool's input schema |
| `400` with field errors | an error result the model can act on |
| auth middleware | permission checks in code |
| `Map<string, handler>` | a dispatcher |
