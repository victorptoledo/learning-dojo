---
from: csharp
status: draft
concept: "Sharing code between repositories"
from_way: "A TFVC branch of a shared folder mapped into several workspaces."
native_way: "A NuGet package from a private feed, or one solution in a monorepo; a submodule only for code you vendor and pin."
sensei_tip: "Visual Studio and dotnet build handle submodules fine, but CI checkouts do not fetch them unless asked (submodules: recursive in GitHub Actions' checkout step)."
---

| .NET habit | Git option |
| --- | --- |
| shared folder mapped in workspaces | a submodule, or better a NuGet package |
| `<ProjectReference>` across solutions | a monorepo |
| a second workspace for a hotfix | `git worktree add` |
| CI checkout | `submodules: recursive` when you use them |
