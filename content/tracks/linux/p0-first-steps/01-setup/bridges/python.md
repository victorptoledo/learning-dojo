---
from: python
status: draft
concept: "Getting and managing a development machine"
from_way: "pip and venv install libraries per project, pyenv picks the interpreter, and on Windows Python comes from python.org or the Store."
native_way: "apt installs system packages shared by every program; Python libraries still go in a venv, never into the system Python with sudo pip."
sensei_tip: "Debian marks its Python as externally managed, so pip refuses to install system-wide. Use apt for tools, python3 -m venv for projects, and pipx for Python command-line tools."
---

| Python | Linux |
| --- | --- |
| pip install requests | sudo apt install python3-requests (system) or pip in a venv |
| python -m venv .venv | the same, after sudo apt install python3-venv |
| pyenv install 3.13 | the distribution's python3, or a container image per version |
| pip list | dpkg -l for system packages |
