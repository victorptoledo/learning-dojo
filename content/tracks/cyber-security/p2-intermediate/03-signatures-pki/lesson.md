# MACs, Digital Signatures, Certificates and TLS

Encryption hides data. This lesson is about the other half: proving that data was not
changed and who produced it.

## MAC: a shared secret

A **message authentication code** is a keyed hash: `tag = HMAC-SHA256(key, message)`. Only
holders of the key can compute a valid tag, so a correct tag proves the message came from one
of them, unchanged. Both sides hold the **same** key, so a MAC cannot prove *which* side
produced a message (no non-repudiation), and anyone who can verify can also forge.

Typical uses: webhooks (the provider signs the body with a secret it shared with you),
signed cookies, and tokens that only your own services verify.

Verifying correctly is where most bugs are:

1. Compute the tag over the **exact bytes** that were signed, usually the raw request body,
   before any JSON parsing and re-serialising.
2. Compare in **constant time** (`hmac.compare_digest`, `MessageDigest.isEqual`,
   `crypto.timingSafeEqual`). An early-exit `==` leaks how many leading bytes matched.
3. Stop **replays**: sign a timestamp together with the body (`t=1731600000` and
   `HMAC(key, t + "." + body)`), reject timestamps outside a few minutes, and optionally
   remember event ids already processed.
4. **Fail closed**: a missing header, a bad hex string or an unknown version means reject.

## Digital signatures: a key pair

A **signature** uses a **private key** to sign and the matching **public key** to verify.
Anyone can verify; only the private-key holder can sign. That gives authenticity and
non-repudiation and makes signatures the tool for software releases, package registries,
documents, certificates and tokens verified by many parties.

| Algorithm | Notes |
| --- | --- |
| **Ed25519** (EdDSA) | modern default: fast, small keys, deterministic, hard to misuse |
| **ECDSA P-256** | widely supported (TLS, WebAuthn, JWT ES256); needs a good random or deterministic nonce |
| **RSA-PSS**, RSA PKCS#1 v1.5 | legacy and compatibility; at least 2048-bit (3072 for long-lived keys) |

Post-quantum signatures are arriving: NIST standardised **ML-DSA** (FIPS 204) and **SLH-DSA**
(FIPS 205) in 2024, and libraries and protocols are adding hybrid modes.

Verification rules: pin the **algorithm** you expect instead of trusting a field in the
message; keep a list of **trusted public keys** (with ids, so you can rotate); and treat any
exception as "invalid". Keep private keys in an **HSM** or KMS, which signs without ever
exporting the key.

## Certificates and the PKI

A public key alone does not say *whose* it is. An **X.509 certificate** binds a public key to
a name (`CN`/`subjectAltName: shop.example.com`), with a validity period, signed by a
**certificate authority (CA)**. Browsers and operating systems ship a set of trusted **root**
CAs; a server sends its certificate plus **intermediate** certificates, and the client builds
a **chain** up to a root, checking every signature, the name, the dates and the key usages.

- **Revocation**: a compromised certificate can be revoked (CRLs, OCSP), but revocation
  checking is unreliable in practice, so the industry moved to **short-lived** certificates:
  by a CA/Browser Forum vote, public TLS certificates may last at most 200 days since March
  2026, 100 days from 2027 and 47 days from 2029. Automate renewal with **ACME** (RFC 8555, Let's Encrypt).
- **Certificate Transparency** (RFC 9162): CAs publish every certificate in public append-only
  logs, so you can monitor for certificates issued for your domains that you did not request.
- **Private PKI**: internal CAs (for example with a service mesh) issue certificates to
  services for **mutual TLS**, where the client also presents a certificate.

## TLS 1.3

TLS gives confidentiality, integrity and server authentication for data in transit. In TLS
1.3 (RFC 8446) the client and server agree a fresh key with ephemeral (EC)DHE, so recorded
traffic stays safe even if the server's long-term key leaks later (**forward secrecy**); the
server proves it owns the certificate's key by **signing** the handshake; only AEAD ciphers
remain. Configuration advice: allow TLS 1.2 and 1.3 only, use the library's modern defaults
rather than hand-picked cipher lists, redirect HTTP to HTTPS and add HSTS (phase 3), and
never disable certificate verification (`verify=False`, `InsecureSkipVerify: true`,
`rejectUnauthorized: false`) outside a test, because that removes the authentication
entirely.

## Summary

- MAC (HMAC): one shared key, integrity and authenticity between two parties; verify the raw
  bytes, in constant time, with a timestamp against replays.
- Signatures (Ed25519, ECDSA, RSA-PSS): private key signs, public key verifies; pin the
  algorithm, trust a list of keys, keep private keys in an HSM or KMS.
- Certificates bind keys to names through CA chains; prefer short-lived, automated
  certificates and watch Certificate Transparency logs.
- TLS 1.3: forward secrecy, signed handshakes, AEAD only; never turn verification off.

## Further reading

- RFC 2104, *HMAC: Keyed-Hashing for Message Authentication*: <https://www.rfc-editor.org/rfc/rfc2104>
- RFC 8032, *Edwards-Curve Digital Signature Algorithm (EdDSA)*: <https://www.rfc-editor.org/rfc/rfc8032>
- RFC 8446, *The Transport Layer Security (TLS) Protocol Version 1.3*: <https://www.rfc-editor.org/rfc/rfc8446>
- RFC 5280, *Internet X.509 Public Key Infrastructure Certificate and CRL Profile*: <https://www.rfc-editor.org/rfc/rfc5280>
- OWASP Transport Layer Security Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Security_Cheat_Sheet.html>
- NIST FIPS 204, *Module-Lattice-Based Digital Signature Standard*: <https://csrc.nist.gov/pubs/fips/204/final>
