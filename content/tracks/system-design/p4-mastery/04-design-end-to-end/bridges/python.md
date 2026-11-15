---
from: python
status: draft
concept: "From components to a whole system"
from_way: "Building one Django or FastAPI app with Celery workers."
native_way: "Deciding, for a whole product, what is synchronous, what is a stream, where the truth lives and how each part degrades."
sensei_tip: "Back-of-the-envelope numbers fit in a Python REPL: 3_000_000 / 86_400 * 4 is your dinner-peak order rate. Keep a small estimates.py next to your design doc so the numbers are reproducible."
---

| Python habit | End-to-end design habit |
| --- | --- |
| one app with Celery | services, streams and queues with owners |
| Django models | data ownership and partition keys per service |
| pytest | load tests (Locust) against SLOs |
| a notebook of quick calculations | written estimates in the design doc |
| `settings.py` | per-region and per-cell configuration |
