---
from: java
status: draft
concept: "Prove origin and integrity: MACs, signatures, certificates"
from_way: "Comparing HMAC hex strings with equals and trusting any certificate with a permissive TrustManager."
native_way: "javax.crypto.Mac with MessageDigest.isEqual, java.security.Signature with Ed25519, and the default TrustManager and hostname verification."
sensei_tip: "The JDK has had Ed25519 since Java 15 (`KeyPairGenerator.getInstance(\"Ed25519\")`, `Signature.getInstance(\"Ed25519\")`); no extra library is needed."
---

| Java | Verify this way |
| --- | --- |
| expectedHex.equals(header) | MessageDigest.isEqual(expected, actual) |
| SHA1withRSA | Ed25519 or SHA256withECDSA |
| a TrustManager that accepts all | the default trust store, or a pinned CA |
| HostnameVerifier returning true | the default verifier |
| keystore password in code | keys in an HSM or KMS |
