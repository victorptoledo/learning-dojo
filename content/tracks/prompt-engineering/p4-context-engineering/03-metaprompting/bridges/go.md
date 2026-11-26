---
from: go
status: draft
concept: "go generate, then go test"
from_way: "go generate with text/template, followed by go vet and go test before the generated code is committed."
native_way: "A metaprompt is go generate for prompts: generate a candidate, vet it (placeholders, format), test it (development set), and keep a holdout set that the generator never sees."
sensei_tip: "text/template fails on a missing key only with Option('missingkey=error'); check prompt placeholders just as strictly, because a template with a missing variable still renders and silently loses its input."
---

| Go | Metaprompting concept |
| --- | --- |
| `go generate` | the optimiser writing a candidate |
| `go vet` | the template-contract check |
| `go test ./...` | scoring on the development set |
| a separate e2e suite | the holdout set |
| a reviewed commit | a new prompt version with a changelog entry |
