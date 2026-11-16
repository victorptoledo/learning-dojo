---
from: java
status: draft
concept: "Injection, again"
from_way: "SQL injection prevented with PreparedStatement, output encoding against XSS, and least-privilege database users."
native_way: "Prompt injection has no prepared statement: escape and delimit untrusted text, mark it as data, and above all limit what the model can do with least privilege, output checks and human confirmation."
sensei_tip: "With SQL, parameters fully separate code from data. With prompts there is no such separation yet, so think of every defence as defence in depth, like running the database user with read-only rights even though you also use prepared statements."
---

| Java security practice | Prompt-injection concept |
| --- | --- |
| `PreparedStatement` parameters | no exact equivalent: escape and delimit instead |
| OWASP Java Encoder for HTML output | escaping text before it enters `<untrusted>` tags |
| a read-only database user | least privilege for tools |
| CSP against XSS exfiltration | checking output for links to unknown domains |
| a confirmation step for transfers | human confirmation before acting |
