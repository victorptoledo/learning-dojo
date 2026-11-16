---
from: python
status: draft
concept: "Security in a dynamic language"
from_way: "pydantic validation, parameterised queries, pickle and yaml.load as code-execution risks, pip-audit, hmac.compare_digest, and secrets.token_hex."
native_way: "Schema validation (zod), parameterised queries, prototype pollution and eval as JavaScript's risks, npm audit, crypto.timingSafeEqual, and crypto.randomBytes."
sensei_tip: "hmac.compare_digest is timingSafeEqual, secrets.token_urlsafe is randomBytes(...).toString(\"base64url\"). Python's equivalent of prototype pollution is rare; in JavaScript it is a top risk."
---

| Python | Node.js |
| --- | --- |
| pydantic model | zod schema |
| `pip-audit` | `npm audit` |
| `hmac.compare_digest(a, b)` | `crypto.timingSafeEqual(a, b)` |
| `secrets.token_hex(32)` | `crypto.randomBytes(32).toString("hex")` |
| `hashlib.scrypt` | `crypto.scrypt` |
| `pickle.loads(untrusted)` | `eval(untrusted)`, unsafe merges |
