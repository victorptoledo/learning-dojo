---
from: java
status: draft
concept: "Virtual environments and dependencies"
from_way: "Dependencies downloaded into a shared, per-user cache and resolved by a build tool."
native_way: "Python isolates dependencies per project in virtual environments (`.venv`). Modern tools such as `uv` install packages much faster than classic `pip`."
sensei_tip: "Standardise projects on `pyproject.toml` (PEP 518/621) instead of the old `setup.py`."
---
