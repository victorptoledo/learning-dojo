---
from: python
status: draft
concept: "Architecture decision records"
from_way: "Decisions are scattered in Slack threads, the README and docstrings, or are rediscovered by reading `requirements.txt`."
native_way: "Numbered Markdown ADRs in `docs/adr`, built into the project docs with Sphinx or MkDocs if you like, reviewed in pull requests and superseded instead of edited."
sensei_tip: "PEPs are decision records for the language itself: context, rationale, rejected ideas, and a status that can be 'Superseded'. Write your ADRs the same way, but shorter."
---

| Python habit | ADR practice |
| --- | --- |
| a long README section | one decision per numbered file |
| PEP-style rationale | ADR context and consequences |
| "Rejected Ideas" in a PEP | considered options with reasons |
| docs built with MkDocs or Sphinx | ADRs published with the other docs |
| `pyproject.toml` dependencies | the outcome; the ADR keeps the why |
