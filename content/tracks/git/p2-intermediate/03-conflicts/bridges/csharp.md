---
from: csharp
status: draft
concept: "Resolving conflicts"
from_way: "Conflicts in .csproj and .sln files when two people add projects or packages."
native_way: "Keep both additions; SDK-style .csproj files list less, so they conflict less than the old format."
sensei_tip: "Central package management (Directory.Packages.props) moves every version into one file. Conflicts move there too, but they become one-line, obvious ones."
---

| .NET situation | What to do |
| --- | --- |
| two `<PackageReference>` lines added | keep both |
| `.sln` conflict with two new projects | keep both entries, check with `dotnet sln list` |
| Visual Studio merge tool | Source / Target are theirs / ours in a merge |
| after resolving | `dotnet build` and `dotnet test` before continuing |
