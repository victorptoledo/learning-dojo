---
from: python
status: draft
concept: "Ask what can go wrong before writing the code"
from_way: "Discovering design problems during a penetration test or after an incident."
native_way: "Lightweight threat models kept next to the code (pytm or a Markdown DFD), reviewed in design pull requests, with abuse-case tests in pytest."
sensei_tip: "OWASP pytm describes the system as Python objects (Actor, Server, Dataflow, Boundary) and generates the DFD and a STRIDE threat list, so the model is versioned and diffed like code."
---

| Python | Threat modeling habit |
| --- | --- |
| whiteboard photo lost in chat | a model in the repository |
| happy-path tests only | pytest abuse cases |
| everyone trusts everyone | explicit trust boundaries |
| ad hoc brainstorming | STRIDE per element |
| risk accepted silently | an owner and a review date |
