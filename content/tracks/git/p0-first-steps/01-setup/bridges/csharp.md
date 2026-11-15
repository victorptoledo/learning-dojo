---
from: csharp
status: draft
concept: "Version control in a .NET solution"
from_way: "Team Foundation Version Control (TFVC): a central server, check-out locks and server workspaces."
native_way: "Git: a full local history, no locks, branches that cost nothing, and bin/ and obj/ ignored."
sensei_tip: "Start from `dotnet new gitignore`: it ignores bin/, obj/, .vs/ and user files. Visual Studio runs the same Git operations, but learn them on the command line first."
---

| TFVC / Visual Studio | Git |
| --- | --- |
| Check out a file to edit it | every file is always editable |
| Server workspace | the local repository in `.git/` |
| Shelvesets | branches or `git stash` |
| Changeset number | commit hash |
| `bin/`, `obj/`, `.vs/` | ignored (`dotnet new gitignore`) |
