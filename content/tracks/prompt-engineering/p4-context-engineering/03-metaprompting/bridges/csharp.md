---
from: csharp
status: draft
concept: "Source generators with guard rails"
from_way: "Roslyn source generators, T4 templates, and analyzers that fail the build when generated code breaks a rule."
native_way: "Let a model generate the prompt, then act like an analyzer: reject candidates that break the template contract, score the rest on a development set, and accept only what also holds on a holdout set."
sensei_tip: "Regex.Matches over the double-brace names plus HashSet.ExceptWith in both directions gives the missing and unknown placeholders, the same check an analyzer would do."
---

| C# | Metaprompting concept |
| --- | --- |
| a Roslyn source generator | a metaprompt |
| an analyzer rule | the template-contract check |
| a test project run in CI | the development evaluation set |
| a staging environment | the holdout set |
| a pull request | a reviewed prompt version |
