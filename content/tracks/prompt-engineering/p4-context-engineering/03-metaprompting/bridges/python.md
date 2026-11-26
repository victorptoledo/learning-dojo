---
from: python
status: draft
concept: "Code that writes code"
from_way: "Code generators, Jinja templates, and property-based tests with Hypothesis that search for failing inputs."
native_way: "A metaprompt is a generator for prompts: check its output like generated code (placeholders, format), score it on a development set, and confirm it on a holdout set before you trust it."
sensei_tip: "re.findall with a pattern for double-brace names gives the placeholders; comparing the two sets with - in both directions names what is missing and what is unknown."
---

| Python | Metaprompting concept |
| --- | --- |
| a code generator plus a linter | a metaprompt plus a template-contract check |
| `string.Template` placeholders | `{{variables}}` the code fills |
| `train_test_split` | development and holdout sets |
| Hypothesis shrinking failures | a capped sample of failures for the optimiser |
| a pull request review | a human reading the generated prompt's diff |
