---
from: javascript
status: draft
concept: "Architecture decision records"
from_way: "Decisions are made in a pull request discussion that is closed and forgotten, and the only trace is a new entry in `package.json`."
native_way: "Numbered Markdown ADRs in the repository (`docs/adr`), proposed in a pull request, linked from the code, and superseded when the decision changes."
sensei_tip: "The JavaScript ecosystem changes fast, so many decisions are two-way doors (a bundler, a test runner). Record them briefly anyway; the record tells the next person it was a choice, not an accident."
---

| JavaScript habit | ADR practice |
| --- | --- |
| a closed pull request thread | an ADR merged with the change |
| `package.json` diff | the outcome; the ADR keeps the why |
| TC39 proposals with stages | ADR status: proposed, accepted, superseded |
| a monorepo README | `docs/adr` per repository or per package |
| tools | `adr-tools`, Log4brains, plain Markdown |
