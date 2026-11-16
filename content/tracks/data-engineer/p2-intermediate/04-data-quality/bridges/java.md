---
from: java
status: draft
concept: "Testing data, not only code"
from_way: "JUnit tests for services and Bean Validation annotations on DTOs."
native_way: "Checks that run on every batch of data (not null, unique, relationships, freshness) with severities and a quality gate."
sensei_tip: "Bean Validation (@NotNull, @Pattern) is row-level validity at the edge of a service; data tests add the batch-level checks (uniqueness, orphans, volume) that one object cannot see."
---

| Java | Data quality |
| --- | --- |
| @NotNull, @Pattern | row-level validity checks |
| a UNIQUE constraint violation | uniqueness, enforced on write |
| JUnit on a transformer | a unit test of pipeline code |
| an exception that stops the job | a quality gate |
| OpenAPI schema in CI | a contract checked by the producer |
