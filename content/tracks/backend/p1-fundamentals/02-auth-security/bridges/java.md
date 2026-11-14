---
from: java
status: draft
concept: "Identity security"
from_way: "Configuring security filters through XML or tangled bean chains."
native_way: "JWTs carry cryptographically signed claims (no database lookup); sessions in Redis allow immediate revocation."
sensei_tip: "Never put sensitive data in a JWT payload: it is only Base64-encoded and anyone can read it."
---
