---
from: java
status: draft
concept: "Configuration versus request data"
from_way: "Application configuration and security rules loaded once at startup, and request parameters handled per call in a controller."
native_way: "The system prompt is the configuration: role, scope, rules, format. The user turn is the request: the user's message and the data fetched for it."
sensei_tip: "A system prompt is like an @PreAuthorize annotation that a clever caller can sometimes talk its way past. Keep the annotation for guidance, and put the real check in the service method."
---

| Java / Spring | Prompt concept |
| --- | --- |
| `application.yaml` | the system prompt |
| `@RequestBody` of a call | the user turn |
| Spring Security filters | enforcing rules in code, not only in the prompt |
| a secret in `application.yaml` read by clients | never put secrets in a system prompt |
| session state | the conversation history resent each call |
