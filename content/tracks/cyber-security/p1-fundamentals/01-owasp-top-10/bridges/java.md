---
from: java
status: draft
concept: "Authorise every object, not just every user"
from_way: "Spring Security protecting URL patterns with roles, and repositories that load any entity by id."
native_way: "Method or query-level ownership checks (`findByIdAndOwnerId`, `@PreAuthorize` with the object) on top of URL rules, and deny by default."
sensei_tip: "`@PreAuthorize(\"@invoices.isOwner(#id, authentication)\")` keeps the decision in one bean you can test; a URL matcher alone cannot see which object is requested."
---

| Java | OWASP Top 10 practice |
| --- | --- |
| repository.findById(id) | findByIdAndOwnerId(id, userId) |
| hasRole('USER') on /invoices/** | an object-level check per request |
| RestTemplate.getForObject(userUrl) | an allow-listed, egress-restricted fetcher (SSRF) |
| ObjectInputStream on input | A08: never deserialise untrusted data |
| stack traces in error responses | A02 and A10: generic errors, logged details |
