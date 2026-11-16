# Verify Signatures

Two classes in `src/main/java/dojo` check signatures, and both trust far too much. Use only
the JDK (`javax.crypto.Mac`, `java.security.Signature`, `MessageDigest`, `HexFormat`,
`Base64`). Both `verify` methods must **never throw**: any malformed input means "not valid".

## `WebhookVerifier`

```java
new WebhookVerifier(List<byte[]> secrets, Duration tolerance, Clock clock)
boolean verify(byte[] rawBody, String signatureHeader)
```

A payment provider sends each webhook with a header like:

```text
t=1794700000,v1=5257a869e7ecebeda32affa62cdca3fa51cad7e77a0e56ff536d0ce8e108d8bd
```

The signature is `HMAC-SHA256(secret, t + "." + rawBody)` in hexadecimal: the timestamp as
written in the header, a dot, then the **raw bytes** of the body. `verify` returns `true`
only if:

- the header is a comma-separated list of `key=value` items (spaces around items are
  ignored), with **exactly one** `t` and **at least one** `v1`; other keys (a future `v2`)
  are ignored;
- `t` is an integer number of seconds and differs from `clock.instant()` by at most
  `tolerance` in either direction (no replays of old webhooks);
- some `v1` value is 64 hexadecimal characters (any case) and equals the HMAC computed with
  **one of** the secrets. Several secrets exist while the provider rotates them. Compare with
  `MessageDigest.isEqual`, never `equals` or `Arrays.equals`.

## `ReleaseVerifier`

```java
new ReleaseVerifier(Map<String, String> trustedKeys)
Optional<String> verify(byte[] artifact, String signatureFile)
void revoke(String keyId)
```

`trustedKeys` maps a key id to an **Ed25519** public key, Base64 of its X.509
SubjectPublicKeyInfo encoding (`PublicKey.getEncoded()`). The constructor throws
`IllegalArgumentException` if any value is not valid Base64 or not an Ed25519 public key
(an RSA or EC key is not accepted).

A signature file has exactly two lines (a final newline is allowed, and `\r\n` line ends
too):

```text
key-id: release-2026
signature: 3dlv...Base64...==
```

`verify` returns the key id if that key is trusted (and not revoked) and the signature, Base64
of 64 raw bytes, is a valid Ed25519 signature of `artifact`; otherwise `Optional.empty()`.
`revoke` removes a key, for example after it leaked; unknown ids are ignored.

Run the visible tests with `mvn test`. There are 13 tests: 2 in `WebhookVerifierTest` and 11
hidden ones.
