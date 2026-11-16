---
from: javascript
status: draft
concept: "The right primitive, through a high-level API"
from_way: "crypto.createHash('sha256') for passwords and Math.random() for ids."
native_way: "argon2 or bcrypt packages (or crypto.scrypt) for passwords, crypto.createCipheriv('aes-256-gcm') or WebCrypto AES-GCM for data, crypto.randomBytes for randomness."
sensei_tip: "With Node's AES-GCM you must call `cipher.getAuthTag()` after encrypting and `decipher.setAuthTag(tag)` before `final()`; forgetting the tag check turns GCM into unauthenticated CTR."
---

| JavaScript / TypeScript | Modern choice |
| --- | --- |
| createHash('sha256').update(pw) | argon2.hash(pw) or crypto.scrypt |
| Math.random() | crypto.randomBytes / getRandomValues |
| createCipher (deprecated) | createCipheriv('aes-256-gcm', key, iv) |
| a === b on MACs | crypto.timingSafeEqual |
| .env committed to git | a secrets manager, .env only locally |
