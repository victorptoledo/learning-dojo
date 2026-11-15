---
from: python
status: draft
concept: "Versions and release tags"
from_way: "__version__ = '1.5.0' edited by hand in __init__.py before each upload to PyPI."
native_way: "Derive the version from the tag with setuptools-scm or hatch-vcs: tag v1.5.0, build, and the package is 1.5.0; between tags it gets a dev version."
sensei_tip: "PyPI never lets you reupload the same version, just as a tag should never move. If 1.5.0 is broken, yank it on PyPI and release 1.5.1."
---

| Python habit | Git |
| --- | --- |
| `__version__` edited by hand | setuptools-scm reads `git describe` |
| `1.5.0.dev12+g3f9a2c1` | 12 commits after the last tag |
| upload to PyPI | push the tag, CI builds and uploads |
| yank a broken release | publish the next tag |
