---
from: python
status: draft
concept: "The right primitive, through a high-level API"
from_way: "hashlib.md5(password).hexdigest() and random.choice for tokens."
native_way: "argon2-cffi or hashlib.scrypt for passwords, AESGCM from pyca/cryptography for data, secrets for randomness, and a secrets manager for keys."
sensei_tip: "The standard library has `hashlib.scrypt` and `hashlib.pbkdf2_hmac` but no AEAD cipher; use the `cryptography` package's `AESGCM` or `ChaCha20Poly1305`, never a hand-made mode."
---

| Python | Modern choice |
| --- | --- |
| hashlib.sha256(pw).hexdigest() | argon2.PasswordHasher().hash(pw) |
| random.random() tokens | secrets.token_urlsafe(32) |
| pycrypto AES.MODE_ECB | cryptography AESGCM |
| == on digests | hmac.compare_digest |
| SECRET_KEY in settings.py | environment from a secrets manager |
