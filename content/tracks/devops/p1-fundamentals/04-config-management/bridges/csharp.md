---
from: csharp
status: draft
concept: "Declare the state, let a tool converge to it"
from_way: "PowerShell scripts or Group Policy configure Windows servers, and some changes happen through Remote Desktop."
native_way: "PowerShell DSC or Ansible (over WinRM) declares the state; a scheduled run reports and repairs drift."
sensei_tip: "PowerShell DSC resources have Test, Set and Get methods: Test is the idempotence check you write in every resource."
---

| C# world | Configuration management |
| --- | --- |
| a PowerShell setup script | a playbook or DSC configuration |
| Group Policy | desired state applied on a schedule |
| `web.config` transforms | templates per environment |
| Remote Desktop fixes | drift |
