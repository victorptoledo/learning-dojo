---
from: csharp
status: draft
concept: "Expose a new version to a few users first"
from_way: "Azure App Service deployment slots: deploy to staging, test, swap."
native_way: "Slot swap is blue-green; a canary splits traffic gradually and decides from metrics, and Microsoft.FeatureManagement adds flags."
sensei_tip: "App Service slots also support routing a percentage of traffic to a slot: that is a canary you may already have."
---

| C# world | Release strategies |
| --- | --- |
| a slot swap | blue-green |
| traffic percentage to a slot | a canary |
| `IFeatureManager` | a feature flag |
| EF Core migrations | expand and contract steps |
