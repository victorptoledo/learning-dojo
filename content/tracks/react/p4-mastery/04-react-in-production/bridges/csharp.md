---
from: csharp
status: draft
concept: "Solutions, analyzers and component libraries"
from_way: ".NET solutions split code into projects, Roslyn analyzers enforce rules, MudBlazor or Fluent UI provide components, and the .NET Upgrade Assistant helps migrations."
native_way: "React apps split code into feature folders or packages, ESLint and TypeScript enforce rules, a design system provides components, and codemods help upgrades."
sensei_tip: "A shared design system package is your MudBlazor; ESLint with react-hooks rules is your Roslyn analyzers. Make warnings errors in CI."
---

| .NET | React |
| --- | --- |
| Projects in a solution | Feature folders or workspace packages |
| Roslyn analyzers | ESLint + TypeScript |
| MudBlazor / Fluent UI | Design system (on Radix, React Aria) |
| .NET Upgrade Assistant | React codemods |
| Application Insights | Error monitoring + Web Vitals |
| bUnit + Playwright | Testing Library + Playwright |
