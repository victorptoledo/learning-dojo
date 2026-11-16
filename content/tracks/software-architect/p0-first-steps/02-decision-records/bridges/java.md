---
from: java
status: draft
concept: "Architecture decision records"
from_way: "Decisions live in a Confluence page nobody updates, or only in the `pom.xml` and the heads of the senior engineers."
native_way: "Numbered Markdown ADRs in `docs/adr` in the same repository, reviewed in pull requests, linked from the code they explain, and superseded rather than edited."
sensei_tip: "A dependency in the `pom.xml` is the outcome of a decision, not the decision. When you add Kafka, Hibernate or a second database, add the ADR in the same pull request."
---

| Java habit | ADR practice |
| --- | --- |
| a wiki page per project | `docs/adr/NNNN-title.md` next to the code |
| "we always used Hibernate" | an ADR that says why, and what would change it |
| Javadoc explaining a workaround | a short comment pointing at the ADR |
| a design review meeting | a proposed ADR reviewed in the pull request |
| tools | `adr-tools`, Log4brains, or plain files |
