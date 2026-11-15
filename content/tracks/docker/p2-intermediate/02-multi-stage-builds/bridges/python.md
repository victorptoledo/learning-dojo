---
from: python
status: draft
concept: "Separate build and runtime environments"
from_way: "Installing packages with C extensions needs a compiler and headers, which then stay on the machine."
native_way: "Build wheels or a virtualenv in a stage that has gcc and headers, then copy only the virtualenv into a slim runtime stage."
sensei_tip: "Create the venv at the same path in both stages (python -m venv /opt/venv, then COPY --from=build /opt/venv /opt/venv) because scripts in a venv hard-code their path."
---

| Python | Multi-stage |
| --- | --- |
| `build-essential` and `libpq-dev` | only in the build stage |
| `pip wheel` or `python -m venv /opt/venv` | built once, copied with `COPY --from` |
| `python:3.13-slim` | the runtime stage |
| musllinux vs manylinux wheels | why Alpine often means compiling from source |
