---
from: python
status: draft
concept: "Logs that a detection can trust"
from_way: "logging.info(f\"login {username}\") into a local text file."
native_way: "Structured logs (structlog or a JSON formatter) with event names and fields, a filter that redacts secrets, and a central collector."
sensei_tip: "A `logging.Filter` attached to the root handler can drop or mask fields named password, token or authorization before anything is written, which protects you from one forgetful log call."
---

| Python | Security logging |
| --- | --- |
| logging.info(f"... {user}") | log.info("login_failed", user_id=..., ip=...) |
| print() for debugging in production | levels and structured fields |
| tokens in exception messages | a redaction filter |
| local rotating files | stdout to a log shipper |
| no request id | contextvars with a trace id |
