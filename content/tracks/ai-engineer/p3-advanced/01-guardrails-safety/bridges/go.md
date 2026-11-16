---
from: go
status: draft
concept: "Untrusted text that gives orders"
from_way: "database/sql placeholders, html/template's contextual escaping and explicit permission checks in handlers."
native_way: "Model output goes through the same escaping and placeholders; tools check permissions from the request context and side effects need approval."
sensei_tip: "html/template escapes model output by context automatically; the danger starts when someone wraps it in template.HTML to render the model's Markdown. Sanitise first (bluemonday) if you must."
---

| Go | LLM guardrail |
| --- | --- |
| `db.Query(sql, args...)` | model output as parameters |
| `html/template` | escaping output |
| bluemonday | sanitising rendered Markdown |
| permissions from `context.Context` | authorisation in tools |
| `slog` with `ReplaceAttr` | masking PII in logs |
