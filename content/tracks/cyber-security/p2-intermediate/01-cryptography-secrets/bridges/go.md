---
from: go
status: draft
concept: "The right primitive, through a high-level API"
from_way: "sha256.Sum256 on passwords and math/rand for tokens."
native_way: "golang.org/x/crypto/argon2 or bcrypt, crypto/cipher with AES-GCM or chacha20poly1305, crypto/rand, and keys from a KMS."
sensei_tip: "`cipher.NewGCM(block)` then `gcm.Seal(nonce, nonce, plaintext, aad)` prepends the nonce to the ciphertext in one call; read it back with `ciphertext[:gcm.NonceSize()]`."
---

| Go | Modern choice |
| --- | --- |
| sha256.Sum256(pw) | argon2.IDKey or bcrypt.GenerateFromPassword |
| math/rand | crypto/rand, rand.Text() |
| cipher.NewCBCEncrypter | cipher.NewGCM |
| bytes.Equal on MACs | hmac.Equal or subtle.ConstantTimeCompare |
| keys in flags or code | a KMS client or mounted secret |
