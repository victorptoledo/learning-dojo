---
from: javascript
status: draft
concept: "Logs that a detection can trust"
from_way: "console.log(req.body) and morgan's combined format in a file on the server."
native_way: "pino or winston with JSON output, redaction paths for secrets, and a request id per request."
sensei_tip: "pino's `redact: ['req.headers.authorization', '*.password']` option removes secrets at serialisation time, faster and safer than remembering to omit them."
---

| JavaScript / TypeScript | Security logging |
| --- | --- |
| console.log(req.body) | logger.info({ event, userId, ip }) |
| Authorization header in access logs | pino redact paths |
| logs on the container filesystem | stdout to the platform collector |
| no correlation | AsyncLocalStorage request id |
| ignored failed logins | an explicit auth_failed event |
