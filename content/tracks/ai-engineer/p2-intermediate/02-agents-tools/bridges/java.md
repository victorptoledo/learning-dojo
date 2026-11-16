---
from: java
status: draft
concept: "The model proposes calls, your code executes them"
from_way: "A controller that dispatches to services based on a request type, with Bean Validation on inputs."
native_way: "Tool definitions with JSON Schemas; the model returns calls; your dispatcher validates the arguments, runs the matching method and returns results by call id."
sensei_tip: "Spring AI's @Tool methods and LangChain4j's @Tool turn annotated Java methods into tool definitions; keep authorisation in the method using the security context, never in its parameters."
---

| Java | Tool calling |
| --- | --- |
| a `Map<String, Handler>` dispatcher | the tool registry |
| `@Valid` request DTO | validating arguments against the schema |
| `@PreAuthorize` | permissions from the session |
| exception handler returning an error body | an error tool result |
| `@Transactional` with an idempotency key | safe side-effecting tools |
