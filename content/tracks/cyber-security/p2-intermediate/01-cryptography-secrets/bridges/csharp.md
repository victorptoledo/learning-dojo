---
from: csharp
status: draft
concept: "The right primitive, through a high-level API"
from_way: "SHA256.Create().ComputeHash on passwords and new Random() for tokens."
native_way: "PasswordHasher (PBKDF2) from ASP.NET Core Identity or an Argon2 library, AesGcm for data, RandomNumberGenerator, and Azure Key Vault or a KMS for keys."
sensei_tip: "`System.Security.Cryptography.AesGcm` takes the nonce, plaintext, ciphertext and tag buffers explicitly, so you see exactly what must be stored: nonce and tag next to the ciphertext."
---

| C# | Modern choice |
| --- | --- |
| SHA256 of the password | PasswordHasher<TUser> or Argon2 |
| new Random() | RandomNumberGenerator.GetBytes |
| AesManaged with CBC | AesGcm |
| SequenceEqual on MACs | CryptographicOperations.FixedTimeEquals |
| secrets in appsettings.json | Key Vault or user secrets |
