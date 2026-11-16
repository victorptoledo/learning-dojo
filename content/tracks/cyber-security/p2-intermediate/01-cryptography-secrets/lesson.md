# Modern Cryptography: Password Hashing, AES-GCM and Secrets

The first rule of applied cryptography: **do not invent it**. Use well-reviewed primitives,
through high-level APIs, with the parameters that current guidance recommends. Most real
failures are not broken algorithms but the wrong tool for the job, a reused nonce, or a key
in a Git repository (A04 Cryptographic Failures).

## Four tools, four jobs

| Tool | Gives | Examples | Use for |
| --- | --- | --- | --- |
| **Hash** | a fixed-size fingerprint; anyone can compute it | SHA-256, SHA-3, BLAKE2 | checksums, content addressing, inside other constructions |
| **Password hash (KDF)** | a *slow*, salted hash | Argon2id, scrypt, bcrypt, PBKDF2 | storing passwords |
| **MAC** | integrity *and* authenticity with a shared key | HMAC-SHA256 | signing webhooks, tokens, cookies (next lessons) |
| **Encryption** | confidentiality (AEAD adds integrity) | AES-GCM, ChaCha20-Poly1305 | data at rest and in transit |

MD5 and SHA-1 are broken for collision resistance: do not use them for anything security
related. Encoding (Base64, hex) is not encryption.

## Storing passwords

A fast hash is the wrong tool: a GPU computes billions of SHA-256 hashes per second, so a
leaked table of unsalted SHA-256 hashes falls to dictionaries in hours, and identical
passwords have identical hashes. A **password hashing function** fixes both:

- a random **salt** per password (16 bytes from a CSPRNG), stored next to the hash, so
  equal passwords hash differently and precomputed tables are useless;
- a tunable **cost**: time, and for Argon2 and scrypt also memory, so each guess is
  expensive for an attacker but takes ~50–250 ms once at login for you.

| Algorithm | OWASP minimum (2025) |
| --- | --- |
| **Argon2id** (first choice) | m = 19 MiB, t = 2, p = 1 |
| **scrypt** | N = 2^17, r = 8, p = 1 |
| **bcrypt** (legacy systems) | cost 10; only the first 72 bytes count |
| **PBKDF2-HMAC-SHA256** (when FIPS is required) | 600 000 iterations |

Store the algorithm and parameters with the hash, in a self-describing string (the PHC
format, `$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>`), so you can raise the cost later: at
each successful login, if the stored parameters are below today's, rehash the password the
user just typed. Compare hashes with a **constant-time** function (`hmac.compare_digest`,
`MessageDigest.isEqual`), so the time taken leaks nothing.

A **pepper** is an extra secret key (kept outside the database, for example in a KMS) mixed
in with HMAC; it protects hashes if only the database leaks.

## Encrypting data

Use **authenticated encryption (AEAD)**: AES-256-GCM or ChaCha20-Poly1305. It encrypts *and*
detects any change to the ciphertext, so an attacker cannot flip bits. Plain CBC or CTR
without a MAC can be tampered with (padding-oracle attacks), and **ECB** leaks patterns.

The one rule you must not break: **never reuse a nonce with the same key**. With GCM, two
messages under the same key and nonce reveal the XOR of the plaintexts and let the attacker
forge messages. Use a random 96-bit nonce per message (fine up to about 2^32 messages per
key) and store it with the ciphertext; it is not secret.

```python
from cryptography.hazmat.primitives.ciphers.aead import AESGCM   # the pyca/cryptography package
key = AESGCM.generate_key(bit_length=256)
nonce = os.urandom(12)
ciphertext = AESGCM(key).encrypt(nonce, b"card on file", b"customer:42")   # last arg: associated data
```

The **associated data** is authenticated but not encrypted: binding the ciphertext to a
record id stops an attacker from copying one customer's ciphertext into another's row.

## Randomness

Keys, salts, nonces, session ids and reset tokens need a **CSPRNG**: `secrets` and
`os.urandom` in Python, `SecureRandom` in Java, `crypto.randomBytes` / `crypto.getRandomValues`
in JavaScript, `RandomNumberGenerator` in .NET, `crypto/rand` in Go. `random`, `Math.random`,
`java.util.Random` and `math/rand` are predictable.

## Keys and secrets

- **Never in code or in Git.** Secrets in a repository are compromised forever (history,
  forks, CI logs); secret scanners (GitHub push protection, gitleaks) catch most of them.
- Keep them in a **secrets manager** or **KMS** (HashiCorp Vault, AWS KMS and Secrets Manager,
  Azure Key Vault, Google Cloud KMS) and give each service access only to its own.
- **Envelope encryption**: a data key encrypts the data; the KMS's master key, which never
  leaves the KMS, encrypts the data key. Rotating the master key does not require
  re-encrypting every document.
- **Rotate** keys on a schedule and immediately after a leak; design for several active key
  versions (a key id next to each ciphertext or token).
- Prefer short-lived credentials (cloud roles, workload identity, OIDC from CI) over
  long-lived API keys.

## Summary

- Hash for fingerprints, a slow salted KDF for passwords, a MAC for authenticity, an AEAD for confidentiality.
- Argon2id (or scrypt, bcrypt, PBKDF2 at OWASP's parameters); store parameters; rehash on login; compare in constant time.
- AES-GCM or ChaCha20-Poly1305 with a unique nonce per message; associated data binds context.
- CSPRNGs only; secrets in a KMS or vault, rotated, never in code.

## Further reading

- OWASP Password Storage Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html>
- OWASP Cryptographic Storage Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html>
- RFC 9106, *Argon2 Memory-Hard Function for Password Hashing*: <https://www.rfc-editor.org/rfc/rfc9106>
- NIST SP 800-38D, *Galois/Counter Mode (GCM)*: <https://csrc.nist.gov/pubs/sp/800/38/d/final>
- OWASP Secrets Management Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html>
