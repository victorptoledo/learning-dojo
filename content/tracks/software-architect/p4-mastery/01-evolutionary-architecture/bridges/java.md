---
from: java
status: draft
concept: "Evolutionary architecture"
from_way: "Architecture rules in a wiki, a rewrite planned on a long-lived `rewrite` branch, and performance checked once before a release."
native_way: "ArchUnit tests with frozen rules (a ratchet) in the build, Gatling or JMH thresholds in the pipeline, OWASP Dependency-Check, SLO alerts from Micrometer metrics, feature toggles for branch by abstraction, and Spring Cloud Gateway or a routing filter as a strangler facade."
sensei_tip: "`FreezingArchRule.freeze(rule)` stores current violations in a store under `archunit_store` and fails only on new ones: a ratchet in one line. Commit the store to the repository."
---

| Java | Evolutionary idea |
| --- | --- |
| ArchUnit, `FreezingArchRule` | structural fitness function with a ratchet |
| Gatling assertions, JMH | performance fitness function |
| OWASP Dependency-Check, Dependabot | security fitness function |
| Micrometer metrics and SLO alerts | continuous fitness function |
| Togglz, Unleash, FF4j | toggles for branch by abstraction |
| Spring Cloud Gateway routes | strangler fig facade |
