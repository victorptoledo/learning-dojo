---
from: csharp
status: draft
concept: "Automating checks around commits"
from_way: "Gated check-ins in TFVC: the server builds the shelveset and only then commits it."
native_way: "Client hooks (dotnet format --verify-no-changes in pre-commit) for fast feedback, plus required checks on the server as the real gate."
sensei_tip: "Husky.Net or a .githooks folder with core.hooksPath works for .NET teams. A dotnet tool restore in the bootstrap script installs both the tools and the hooks."
---

| TFVC / .NET | Git |
| --- | --- |
| Gated check-in | required status checks on the pull request |
| Check-in policies (comment required) | `commit-msg` hook + a CI check |
| `dotnet format` before check-in | `pre-commit` |
| Server-side check-in notification | webhooks |
