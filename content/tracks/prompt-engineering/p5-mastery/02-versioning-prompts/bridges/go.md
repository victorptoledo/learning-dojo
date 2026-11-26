---
from: go
status: draft
concept: "Modules and pinned versions"
from_way: "go.mod with exact module versions, go.sum for verification, and major versions in the import path."
native_way: "Each prompt version pins its template, model snapshot, parameters and schema; a major version changes the output contract and ships with the code that parses it."
sensei_tip: "Go puts a major version in the import path because it is a different contract; do the same with prompts: ticket-triage/v3 is not a drop-in replacement for v2."
---

| Go | Prompt-versioning concept |
| --- | --- |
| `go.mod` with exact versions | pinned model snapshot |
| `/v2` in the import path | a major prompt version |
| `embed` of prompt files | prompts kept as versioned files |
| `slog` attributes on each call | logging the prompt version |
| a config flag | selecting a version at run time |
