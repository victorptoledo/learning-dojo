# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a login that checks scrypt password hashes and
   issues signed tokens, with complete validation and key rotation.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Modern cryptography | Why a slow, salted KDF? What must never repeat with AES-GCM? Where do keys live? |
| Password storage | What makes a stored hash malformed? How do you upgrade old hashes without the plaintext? |
| Signatures, certificates, TLS | MAC or signature? Why the raw bytes, constant time and a timestamp? What does a CA sign? |
| Tokens and API security | Which claims must you check? Why pin the algorithm? In which order do you rotate keys? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
