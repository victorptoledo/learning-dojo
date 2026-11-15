---
from: java
status: draft
concept: "Code smells and refactoring"
from_way: "Refactoring by hand in a text editor, big-bang rewrites, and `Utils` classes that collect everything."
native_way: "Characterisation tests first, then the IDE's automated refactorings in small committed steps; records, enums and sealed types for primitive obsession and type codes; SonarQube or PMD to find candidates."
sensei_tip: "IntelliJ's Extract Method, Introduce Parameter Object and Replace Constructor with Builder are behaviour-preserving when run by the IDE; typing the same change by hand is where bugs come in."
---

| Java | Refactoring tool or idea |
| --- | --- |
| IntelliJ / Eclipse refactorings | automated moves |
| `record`, `enum`, `sealed` | fix primitive obsession, type codes |
| ApprovalTests, golden files | characterisation tests |
| SonarQube, PMD, Error Prone | smell detection |
| ArchUnit | keeping structure from rotting |
