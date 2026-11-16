---
from: csharp
status: draft
concept: "Developers own their service in production"
from_way: "You publish from Visual Studio or hand an MSI to the infrastructure team, who deploy it to IIS during a maintenance window."
native_way: "`dotnet publish` runs in a pipeline, produces an image, and the same pipeline deploys it; the team watches Application Insights or Prometheus afterwards."
sensei_tip: "Azure DevOps and GitHub Actions are both first-class for .NET; learn the pipeline YAML, not only the portal."
---

| C# world | DevOps |
| --- | --- |
| right-click Publish | a deploy job in CI/CD |
| IIS on a VM | containers on Kubernetes or App Service |
| a release window | small, frequent, automated releases |
| Event Viewer | centralised logs and alerts |
