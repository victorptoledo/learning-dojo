---
from: javascript
status: draft
concept: "Codemods and their tests"
from_way: "jscodeshift codemods, template literals, and snapshot tests that catch unintended changes."
native_way: "A metaprompt rewrites prompts like a codemod rewrites code: the template contract is the API that must not change, the evaluation set is the test suite, and the holdout set catches changes that only fit the cases you saw."
sensei_tip: "Compare placeholder sets with new Set(...) in both directions; a candidate that drops {{input}} should be rejected before a single model call is spent scoring it."
---

| JavaScript | Metaprompting concept |
| --- | --- |
| a jscodeshift codemod | a metaprompt rewriting a prompt |
| template literal `${name}` | `{{name}}` placeholders |
| snapshot tests | the development evaluation set |
| a separate e2e suite | the holdout set |
| reviewing the codemod's diff | reviewing the generated prompt |
