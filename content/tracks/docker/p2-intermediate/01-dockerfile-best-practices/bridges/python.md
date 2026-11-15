---
from: python
status: draft
concept: "Incremental builds and dependency caching"
from_way: "pip reuses its download cache in ~/.cache/pip and a virtualenv keeps installed packages between runs."
native_way: "Copy requirements.txt (or the lock file) alone, install with --no-cache-dir in one layer, then copy the code; the install layer is reused until the requirements change."
sensei_tip: "Use a lock file with hashes (pip-tools, uv or Poetry export) so the cached layer and a fresh build install the same versions."
---

| Python | Dockerfile |
| --- | --- |
| `~/.cache/pip` | not wanted in the image: `--no-cache-dir` |
| `requirements.txt` | copied before the code |
| `pip install -e .` in development | `pip install .` in a build step |
| flake8 or ruff | hadolint (and ShellCheck rules inside `RUN`) |
