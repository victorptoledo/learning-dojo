---
from: go
status: draft
concept: "Structuring a Spring application"
from_way: "Packages per domain with internal/ directories enforcing visibility."
native_way: "Modules as packages, with Spring Modulith enforcing what Go's internal/ enforces."
sensei_tip: "Go's internal/ directory is a compiler-checked boundary; Spring Modulith gives you a test-checked one with the same intent."
---

| Go | Spring Boot 4 |
| --- | --- |
| internal/ packages | internal subpackages of a module |
| package per domain | module per business capability |
| channels between components | application events |
| go vet / linters | Spring Modulith verify() |
