---
from: csharp
status: draft
concept: "Instances are disposable; build them from images and give them roles"
from_way: "An IIS server is configured by hand with the app pool, certificates and a service account."
native_way: "A launch template (AMI, instance profile, user data or SSM documents) builds identical Windows or Linux instances in an Auto Scaling group."
sensei_tip: "Run ASP.NET Core on Linux Graviton instances where you can; Windows AMIs cost more for the license and boot slower in scale-out."
---

| .NET world | AWS |
| --- | --- |
| an IIS server you care for | an Auto Scaling group from a launch template |
| Group Policy | SSM State Manager and documents |
| a service account password | an instance profile |
| Remote Desktop | Fleet Manager or Session Manager |
