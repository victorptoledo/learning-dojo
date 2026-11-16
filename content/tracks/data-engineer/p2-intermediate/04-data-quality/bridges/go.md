---
from: go
status: draft
concept: "Testing data, not only code"
from_way: "Table-driven tests for functions and validation in handlers."
native_way: "Checks over whole batches with failing-row samples, severities and a gate before publishing."
sensei_tip: "Table-driven tests are a natural fit for pipeline transformations: each case is a tiny input dataset and the expected rows."
---

| Go | Data quality |
| --- | --- |
| table-driven tests | unit tests with edge-case inputs |
| go-playground/validator tags | row-level validity |
| a map[key]int counter | a unique check |
| errors.Join of problems | a report of failed checks |
| a non-zero exit code | a quality gate in a scheduler |
