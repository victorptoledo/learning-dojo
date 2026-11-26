---
from: python
status: draft
concept: "Prompts as package data"
from_way: "Prompt strings hard-coded in modules, changed in place."
native_way: "Prompt files with metadata loaded at start, a version that pins the model and parameters, a changelog, and the version logged with each call."
sensei_tip: "Load prompts with importlib.resources from a prompts package and validate their metadata with a dataclass at start-up; a bad prompt file then fails fast, not on the first request."
---

| Python | Prompt-versioning concept |
| --- | --- |
| `importlib.resources.files("prompts")` | prompts kept as files |
| `uv.lock` pinning versions | pinning the model snapshot |
| `__version__` | the prompt version |
| `logging` with `extra={"prompt_version": ...}` | tracing calls to a version |
| an environment flag | selecting a version at run time |
