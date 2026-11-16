---
from: csharp
status: draft
concept: "Declare the infrastructure and let the engine converge it"
from_way: "Environments are built by clicking through a wiki page, so staging and production differ."
native_way: "CloudFormation templates or a C# CDK app declare everything; the same code builds every environment."
sensei_tip: "The CDK in C# uses the same constructs as TypeScript; keep it in its own project in the solution and run `cdk synth` in the build pipeline."
---

| .NET world | AWS |
| --- | --- |
| ARM templates or Bicep | CloudFormation templates |
| a release pipeline in Azure DevOps | CodePipeline or GitHub Actions running `cdk deploy` |
| `appsettings.Production.json` | parameters and SSM parameters |
| a NuGet package of shared code | a CDK construct library |
