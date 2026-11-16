---
from: javascript
status: draft
concept: "Small, reversible changes; practised incidents; blameless learning"
from_way: "Merge to main deploys straight to all users at once."
native_way: "Deploy to a canary slice first, watch alarms, roll back automatically, then widen."
sensei_tip: "Use Lambda aliases with `DeploymentPreference: { Type: Canary10Percent5Minutes, Alarms: [...] }` in SAM; it is the cheapest progressive delivery you will ever set up."
---

| Node.js world | AWS |
| --- | --- |
| Vercel instant rollback | CodeDeploy alarm-based rollback |
| LaunchDarkly flags | AppConfig feature flags |
| a status page | the same, fed from Incident Manager |
| chaos monkey scripts | Fault Injection Service experiments |
