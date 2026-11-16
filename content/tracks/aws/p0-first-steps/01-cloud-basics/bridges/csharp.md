---
from: csharp
status: draft
concept: "Everything in AWS is an API you call with signed credentials"
from_way: "An ASP.NET app runs on IIS servers in your own datacentre, patched by the ops team."
native_way: "The AWS SDK for .NET calls AWS APIs; for each service you decide whether you or AWS runs the OS and the engine."
sensei_tip: "Register clients with `services.AddAWSService<IAmazonS3>()` from AWSSDK.Extensions.NETCore.Setup; it reads the profile locally and the role in AWS."
---

| .NET world | AWS |
| --- | --- |
| Windows Server and IIS you patch | EC2 (yours) or Elastic Beanstalk/ECS/Lambda (less of yours) |
| Active Directory logins | IAM Identity Center (it can federate with Entra ID) |
| `appsettings.json` secrets | roles and Secrets Manager |
| one datacentre | regions with several Availability Zones |
