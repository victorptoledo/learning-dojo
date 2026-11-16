---
from: csharp
status: draft
concept: "Prove origin and integrity: MACs, signatures, certificates"
from_way: "Comparing Base64 strings and a ServerCertificateCustomValidationCallback that returns true."
native_way: "HMACSHA256 with CryptographicOperations.FixedTimeEquals, ECDsa or Ed25519 through a library, and default certificate validation."
sensei_tip: "`HttpClientHandler.DangerousAcceptAnyServerCertificateValidator` is named that way on purpose; if you find it outside test code, you have found a vulnerability."
---

| C# | Verify this way |
| --- | --- |
| computed == header | CryptographicOperations.FixedTimeEquals |
| validation callback returning true | the default validation, or a custom trust store |
| RSA with SHA1 | ECDsa with SHA256, or Ed25519 |
| certificates renewed by hand | ACME automation |
| private keys in .pfx files on disk | Key Vault or an HSM |
