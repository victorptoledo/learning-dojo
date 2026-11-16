---
from: csharp
status: draft
concept: "Security as a program the teams trust"
from_way: "Security handled by a separate team at the end of each release, with findings in spreadsheets."
native_way: "Microsoft SDL practices in the pipeline: Roslyn security analyzers, dependency and secret scanning, threat models for risky features, and findings as work items with SLAs."
sensei_tip: "The .NET SDK's built-in analyzers include security rules (the CA2xxx and CA3xxx series); raising their severity in .editorconfig puts SAST in every build at no extra cost."
---

| C# | Program practice |
| --- | --- |
| spreadsheet of findings | work items with owners and SLAs |
| security sign-off at the end | checks in every pull request |
| analyzers at default severity | security rules raised in .editorconfig |
| ISO audit scramble | evidence collected continuously |
| no metrics | time to fix, coverage, KEV exposure |
