---
from: javascript
status: draft
concept: "Prove origin and integrity: MACs, signatures, certificates"
from_way: "Comparing digests with === and setting NODE_TLS_REJECT_UNAUTHORIZED=0 when certificates fail."
native_way: "crypto.createHmac with crypto.timingSafeEqual, crypto.verify with Ed25519 keys, and TLS verification always on."
sensei_tip: "Express's `express.raw({ type: 'application/json' })` on the webhook route keeps the body as a Buffer, which is what the HMAC must be computed over."
---

| JavaScript / TypeScript | Verify this way |
| --- | --- |
| digest === header | crypto.timingSafeEqual(a, b) |
| express.json() before the HMAC | express.raw() on that route |
| NODE_TLS_REJECT_UNAUTHORIZED=0 | add the CA with NODE_EXTRA_CA_CERTS |
| rejectUnauthorized: false | the default true |
| jsrsasign by hand | crypto.sign(null, data, ed25519Key) |
