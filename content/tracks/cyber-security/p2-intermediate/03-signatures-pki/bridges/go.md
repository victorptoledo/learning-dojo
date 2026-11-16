---
from: go
status: draft
concept: "Prove origin and integrity: MACs, signatures, certificates"
from_way: "bytes.Equal on MACs and tls.Config{InsecureSkipVerify: true} to get past errors."
native_way: "hmac.Equal, crypto/ed25519 Sign and Verify, and the default tls.Config with a custom RootCAs pool only when needed."
sensei_tip: "Go's `crypto/ed25519` is in the standard library: `ed25519.Verify(pub, msg, sig)` returns a bool and never panics on bad input lengths for the message."
---

| Go | Verify this way |
| --- | --- |
| bytes.Equal(mac, sig) | hmac.Equal(mac, sig) |
| InsecureSkipVerify: true | RootCAs with your CA |
| rsa.SignPKCS1v15 for new designs | ed25519.Sign |
| MinVersion unset on old Go | MinVersion: tls.VersionTLS12 |
| reading the body twice | io.ReadAll once, verify, then decode |
