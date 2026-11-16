---
from: csharp
status: draft
concept: "Small, reversible changes; practised incidents; blameless learning"
from_way: "Azure DevOps release gates and deployment slots."
native_way: "CodePipeline or GitHub Actions across accounts, with canary and blue/green deployments gated by CloudWatch alarms."
sensei_tip: "Deployment slots map to ECS blue/green or Lambda aliases; keep the swap automatic on alarms rather than a manual button."
---

| .NET world | AWS |
| --- | --- |
| deployment slots | blue/green with test traffic |
| release gates | alarms and bake time |
| Azure Automation | Systems Manager Automation |
| Azure Service Health | AWS Health |
