---
from: python
status: draft
concept: "From pip, virtualenv and requirements.txt to uv"
from_way: "pyenv for interpreters, python -m venv plus activate, pip install -r requirements.txt, pip freeze to pin, pipx for tools and setup.py for packaging."
native_way: "One tool: uv python install, uv add, uv run, uv lock and uvx, with everything described in pyproject.toml and pinned in uv.lock."
sensei_tip: "An old project moves in an afternoon: uv init --bare, uv add -r requirements.txt, uv add --dev -r requirements-dev.txt, commit pyproject.toml and uv.lock, and drop setup.py once there is a [build-system] table."
---

| Older habit | Python 3.15 with uv |
| --- | --- |
| `pyenv install 3.11` | `uv python install 3.15` (or `.python-version`) |
| `python -m venv .venv` + `activate` | created by `uv run` / `uv sync`, no activation |
| `pip install requests`, then edit `requirements.txt` | `uv add requests` |
| `pip freeze > requirements.txt` | `uv lock` (`uv export` if a tool needs the old format) |
| `requirements-dev.txt` | `uv add --dev pytest` |
| `pipx run black` | `uvx ruff format` |
| `setup.py` / `setup.cfg` | `pyproject.toml` |
