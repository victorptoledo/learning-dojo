---
from: go
status: draft
concept: "Accessibility as part of correctness"
from_way: "Server-rendered templates, where accessibility is whatever markup the template contains."
native_way: "The same templates can be fully accessible: native elements, labels, headings and live regions cost nothing at runtime."
sensei_tip: "Like go vet, axe-core finds the obvious mistakes cheaply; run it in tests so regressions fail the build."
---

| Go | In the browser |
| --- | --- |
| `go vet` in CI | axe-core in the test suite |
| A clear error message | An error tied with `aria-describedby` |
| Explicit, simple code | Native elements over custom widgets |
| Table-driven tests | Tabbing through every control |
