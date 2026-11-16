---
from: java
status: draft
concept: "Modularity and package dependencies"
from_way: "Packages by technical layer, every class `public`, and a `common` module that every Maven module depends on."
native_way: "Packages and Maven or Gradle modules that hide a decision each, package-private classes by default, JPMS `exports` or Spring Modulith for published interfaces, and ArchUnit tests against cycles."
sensei_tip: "Java's default visibility is package-private: drop `public` from every class that is not part of the package's interface. It is the cheapest information hiding you will ever get."
---

| Java | Modularity idea |
| --- | --- |
| package-private classes | information hiding inside a package |
| `module-info.java` with `exports` | a module's published interface |
| Maven or Gradle modules | components with enforced acyclic dependencies |
| ArchUnit `slices().should().beFreeOfCycles()` | the acyclic dependencies principle as a test |
| JDepend, Structure101, Sonargraph | Ca, Ce, instability and abstractness |
| a `common` jar used by everything | the zone of pain |
