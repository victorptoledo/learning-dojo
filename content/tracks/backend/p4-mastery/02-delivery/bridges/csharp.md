---
from: csharp
status: draft
concept: "Shipping changes"
from_way: "Azure DevOps or GitHub Actions pipelines and EF migrations."
native_way: "DORA metrics, progressive delivery and backward-compatible schema changes."
sensei_tip: "EF migrations run before the new code is everywhere; make each migration compatible with both the old and the new version."
---

| C# | In practice |
| --- | --- |
| EF migration rename | expand-and-contract |
| release pipeline | automated, small batches |
| deployment slots | blue-green |
| Microsoft.FeatureManagement | feature flags |
