---
from: python
status: draft
concept: "Modular monolith"
from_way: "One Django project where every app imports every other app's models and queries them directly."
native_way: "Django apps or packages per capability, each with a small public module (`api.py` or `__init__` re-exports), `import-linter` contracts that forbid imports of internals, and signals or an in-process event bus between modules."
sensei_tip: "Django's ForeignKey across apps is the most common boundary leak. Reference another module's records by id and ask its api for the details."
---

| Python | Modular monolith idea |
| --- | --- |
| a Django app or package per capability | modules |
| `api.py`, `__all__` | published interface |
| `import-linter` `independence` and `forbidden` contracts | boundaries as a check |
| Django signals, `blinker`, a small event bus | in-process events |
| one schema or table prefix per app | data ownership |
| a ForeignKey into another app | a leak to remove |
