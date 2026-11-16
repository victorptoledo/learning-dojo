---
from: csharp
status: draft
concept: "Operate it for years, not weeks"
from_way: "Moving between .NET LTS versions, deployment slots for safe releases and Application Insights for operations."
native_way: "Model migrations with evaluation, staged rollout through slots or flags, telemetry on quality and cost, and responsible-AI documentation."
sensei_tip: "Deployment slots map well to model migrations: deploy the new model configuration to a slot, send shadow or a small share of traffic, compare telemetry, then swap, with the old slot as the rollback."
---

| .NET operations | AI feature operations |
| --- | --- |
| .NET LTS upgrades | model upgrades |
| deployment slots | staged model rollout |
| `Microsoft.FeatureManagement` | prompt and model flags |
| Application Insights | quality, cost and latency telemetry |
| compliance documentation | system cards and risk assessments |
