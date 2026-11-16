---
from: java
status: draft
concept: "Application security, JavaScript edition"
from_way: "Bean Validation at the controller, PreparedStatement against SQL injection, deserialisation gadgets as a classic risk, OWASP Dependency-Check, and the (removed) SecurityManager."
native_way: "Schema validation at the boundary, parameterised queries, prototype pollution and ReDoS as JavaScript-specific risks, npm audit and lockfiles, and the --permission model."
sensei_tip: "Prototype pollution is to JavaScript what deserialisation gadgets are to Java: data that changes the behaviour of unrelated code. And --permission covers a small part of what SecurityManager once tried to do."
---

| Java | Node.js |
| --- | --- |
| `@Valid` + Bean Validation | zod / JSON Schema at the boundary |
| `PreparedStatement` | Parameterised queries (`$1`) |
| Deserialisation gadgets | Prototype pollution |
| OWASP Dependency-Check | `npm audit`, Dependabot |
| `MessageDigest.isEqual` | `crypto.timingSafeEqual` |
| SecurityManager (removed) | `--permission` |
