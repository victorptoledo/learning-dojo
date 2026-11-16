---
from: csharp
status: draft
concept: "The pipeline is a build file for the whole delivery"
from_way: "An Azure DevOps classic build definition edited in the browser."
native_way: "A YAML pipeline in the repository runs dotnet restore, build and test, with the NuGet cache keyed by packages.lock.json."
sensei_tip: "Enable RestorePackagesWithLockFile so NuGet has a lock file to cache on and builds are reproducible."
---

| C# world | CI pipeline |
| --- | --- |
| a classic build definition | pipeline YAML in the repo |
| `dotnet test --logger trx` | a test report artefact |
| the NuGet global packages folder | a cache |
| a solution with many projects | jobs per project or path filters |
