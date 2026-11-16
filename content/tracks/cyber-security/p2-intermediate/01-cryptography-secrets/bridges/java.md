---
from: java
status: draft
concept: "The right primitive, through a high-level API"
from_way: "MessageDigest.getInstance(\"MD5\") for passwords and Cipher.getInstance(\"AES\"), which silently means AES/ECB."
native_way: "A password encoder (Argon2 or bcrypt from Spring Security), AES/GCM/NoPadding with a fresh 12-byte IV, SecureRandom, and keys from a KMS."
sensei_tip: "`Cipher.getInstance(\"AES\")` defaults to ECB with PKCS5 padding in the JDK; always spell out `AES/GCM/NoPadding` and pass a `GCMParameterSpec(128, iv)`."
---

| Java | Modern choice |
| --- | --- |
| MessageDigest SHA-256 of the password | Argon2PasswordEncoder or BCryptPasswordEncoder |
| Cipher.getInstance("AES") | "AES/GCM/NoPadding" with a new IV |
| new Random() | SecureRandom |
| key in application.yml | a KMS or Vault, injected at runtime |
| String.equals on hashes | MessageDigest.isEqual |
