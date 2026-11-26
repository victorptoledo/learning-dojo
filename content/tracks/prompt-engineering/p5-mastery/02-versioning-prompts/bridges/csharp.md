---
from: csharp
status: draft
concept: "Versioned contracts"
from_way: "NuGet packages with semantic versions, API versioning in ASP.NET, and configuration per environment."
native_way: "Prompt versions with an explicit contract (template, model snapshot, parameters, tools, schema), a version number that signals breaking changes, and rollout by configuration."
sensei_tip: "Treat a major prompt version like a new API version: the output contract changed, so the parsing code ships with it, and the old version stays available until callers have moved."
---

| C# | Prompt-versioning concept |
| --- | --- |
| a NuGet package version | a prompt version |
| `[ApiVersion("2.0")]` | a major version with a new output contract |
| `appsettings.{Environment}.json` | which version each environment uses |
| `Microsoft.FeatureManagement` | selecting a version at run time |
| release notes | the prompt changelog |
