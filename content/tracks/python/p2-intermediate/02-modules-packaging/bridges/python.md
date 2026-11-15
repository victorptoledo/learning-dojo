---
from: python
status: draft
concept: "From setup.py to pyproject.toml"
from_way: "setup.py with setuptools.setup(), setup.cfg, MANIFEST.in, python setup.py sdist bdist_wheel, twine upload, console_scripts entry_points, pkg_resources and implicit relative imports."
native_way: "pyproject.toml with [project] and [build-system], uv build and uv publish, [project.scripts], importlib.metadata and importlib.resources, and absolute imports in a src layout."
sensei_tip: "Running setup.py directly is deprecated, and pkg_resources is deprecated too: move metadata into [project] and read versions with importlib.metadata.version()."
---

| Older habit | Python 3.15 |
| --- | --- |
| `setup.py` with `setup(name=..., install_requires=[...])` | `[project]` with `dependencies = [...]` |
| `python setup.py sdist bdist_wheel` | `uv build` |
| `twine upload dist/*` | `uv publish` |
| `entry_points={"console_scripts": [...]}` | `[project.scripts]` |
| `pkg_resources.get_distribution("x").version` | `importlib.metadata.version("x")` |
| `open(os.path.join(os.path.dirname(__file__), "data.json"))` | `importlib.resources.files("pkg") / "data.json"` |
| `import utils` meaning `.utils` (Python 2) | `from pkg import utils` or `from . import utils` |
| `pip install -e .` | `uv sync` (editable by default) |
