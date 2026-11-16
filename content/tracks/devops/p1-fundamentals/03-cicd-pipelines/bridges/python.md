---
from: python
status: draft
concept: "The pipeline is a build file for the whole delivery"
from_way: "tox or nox runs lint and tests across Python versions on your laptop."
native_way: "A matrix runs the same sessions on fresh runners for each Python version, with pip or uv caches keyed by the lock file."
sensei_tip: "setup-python's cache: pip (or uv's cache) needs a lock file to key on; without one every run downloads the world."
---

| Python world | CI pipeline |
| --- | --- |
| `tox -e py313,py314` | a matrix over Python versions |
| `pre-commit run --all-files` | a lint job |
| `~/.cache/pip` | a cache keyed by the lock file |
| `pytest --junitxml` | a test report artefact |
