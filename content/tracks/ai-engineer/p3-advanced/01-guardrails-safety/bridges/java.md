---
from: java
status: draft
concept: "Untrusted text that gives orders"
from_way: "SQL injection and XSS: untrusted input mixed into code or markup, fixed with prepared statements and encoding."
native_way: "Prompt injection mixes untrusted text into the model's instructions, and there is no prepared statement for prompts, so you limit privileges and check actions instead."
sensei_tip: "Apply the controls you know from Spring Security to tools: method security on every tool, the principal from the session, audit logs, and confirmation flows for state-changing operations."
---

| Java security | LLM equivalent |
| --- | --- |
| `PreparedStatement` | no full equivalent: delimit untrusted text and limit privileges |
| `@PreAuthorize` on services | authorisation inside every tool |
| OWASP Java Encoder | escaping model output before rendering |
| a confirmation step before transfers | human approval for consequential tool calls |
| log masking (Logback converters) | redacting PII in prompts and traces |
