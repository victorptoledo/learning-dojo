---
from: python
status: draft
concept: "Version control in a Python project"
from_way: "A project folder with the virtual environment, __pycache__ and notebook outputs all copied together."
native_way: "Commit the sources, pyproject.toml and a lock file; ignore .venv/, __pycache__/ and build artefacts."
sensei_tip: "Never commit a virtual environment: it contains absolute paths and platform binaries. The lock file (uv.lock, poetry.lock) is what reproduces it."
---

| Python habit | With Git |
| --- | --- |
| Zip the folder with `venv/` | ignore `.venv/`, commit `pyproject.toml` and the lock file |
| `__pycache__/`, `*.pyc` | ignored |
| Notebook outputs | strip outputs before committing (nbstripout) |
| `pip freeze > requirements.txt` by hand | the lock file changes in the same commit as the code |
