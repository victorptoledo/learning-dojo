---
from: java
status: draft
concept: "Logs that a detection can trust"
from_way: "log.info(\"login \" + username + \" ok\") with Logback text patterns, and request bodies logged at debug level."
native_way: "Structured events (SLF4J key-value pairs or a JSON encoder) with fixed fields, no secrets, and shipping to a central store."
sensei_tip: "SLF4J 2's fluent API, `log.atInfo().addKeyValue(\"event\", \"login_failed\").addKeyValue(\"user\", id).log()`, with a JSON encoder makes newline injection a non-issue."
---

| Java | Security logging |
| --- | --- |
| string concatenation in log messages | key-value fields, JSON output |
| logging the request body | log ids and outcomes, never credentials |
| local files only | a shipper to central storage |
| printStackTrace() | logged errors with a request id |
| no audit events | an AuditEventRepository or explicit audit logger |
