---
from: python
status: draft
concept: "Packaging an application with its runtime"
from_way: "A virtualenv isolates Python packages, but not the interpreter version, system libraries such as libpq or the operating system."
native_way: "An image fixes the interpreter, the system libraries and the installed packages together, so the same image runs everywhere."
sensei_tip: "Start from python:3.13-slim rather than the full image: it has the interpreter without compilers and documentation, and is several times smaller."
---

| Python | Docker |
| --- | --- |
| `python -m venv .venv` | an image is a frozen, shareable environment |
| `requirements.txt` | installed in a build step: `RUN pip install -r requirements.txt` |
| `pyenv install 3.13` | `FROM python:3.13-slim` |
| "works on my laptop" | the same image in dev, CI and production |
