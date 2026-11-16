---
from: java
status: draft
concept: "Naming and representation conventions"
from_way: "camelCase Java fields leaking as-is, `Double` for money, `java.util.Date` serialised as epoch milliseconds."
native_way: "Records with deliberate names, `long` minor units or `BigDecimal` as string for money, `Instant`/`LocalDate` serialised as RFC 3339 strings."
sensei_tip: "Jackson can write `BigDecimal` as a string (`@JsonFormat(shape = STRING)`) and Spring Boot writes `java.time` types as ISO strings by default; never let money become a JSON double."
---

| Java / Spring | Convention |
| --- | --- |
| `Instant` → `"2026-03-01T10:00:00Z"` | RFC 3339 timestamp |
| `long priceCents` + `String currency` | money |
| `enum Status` + docs saying it is open | open enum |
| `PropertyNamingStrategies.SNAKE_CASE` | one case everywhere |
| `record Page<T>(List<T> items, String nextCursor)` | collection envelope |
