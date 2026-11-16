---
from: csharp
status: draft
concept: "Encrypt, keep secrets out of code, record everything, detect threats"
from_way: "Azure Key Vault and Microsoft Defender for Cloud cover keys, secrets and threats."
native_way: "KMS and Secrets Manager hold keys and secrets; GuardDuty, Inspector and Security Hub detect and score."
sensei_tip: "Add `AWSSDK.Extensions.NETCore.Setup` and the Secrets Manager configuration provider so secrets arrive through `IConfiguration`, never in appsettings files."
---

| .NET world | AWS |
| --- | --- |
| Key Vault keys | KMS keys |
| Key Vault secrets | Secrets Manager |
| Defender for Cloud | Security Hub and GuardDuty |
| Azure Activity Log | CloudTrail |
