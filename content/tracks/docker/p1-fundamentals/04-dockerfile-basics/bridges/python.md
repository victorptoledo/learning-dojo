---
from: python
status: draft
concept: "A declarative build recipe"
from_way: "pyproject.toml declares the package and its entry points; how it is installed and run on a server is a separate script."
native_way: "The Dockerfile installs the interpreter dependencies and your package, and declares the command: ENTRYPOINT [\"python\", \"-m\", \"app\"] or your console script."
sensei_tip: "Set ENV PYTHONUNBUFFERED=1 so print and logging output reach docker logs immediately, and PYTHONDONTWRITEBYTECODE=1 to keep .pyc files out of the layers."
---

| Python | Dockerfile |
| --- | --- |
| `[project.scripts] tool = "app:main"` | `ENTRYPOINT ["tool"]` |
| `argparse` defaults | `CMD` default arguments |
| `.gitignore` | `.dockerignore` (also exclude `.venv`, `__pycache__`) |
| `os.environ.get("PORT", "8000")` | `ENV PORT=8000`, overridden with `-e` |
