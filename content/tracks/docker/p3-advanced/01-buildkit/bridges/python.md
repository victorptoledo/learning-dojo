---
from: python
status: draft
concept: "Build caches, build secrets and target platforms"
from_way: "pip caches wheels in ~/.cache/pip and reads private index credentials from pip.conf or environment variables."
native_way: "A cache mount on /root/.cache/pip keeps wheels between builds, and a secret mount provides pip.conf for one step."
sensei_tip: "Wheels are platform-specific: an arm64 image needs arm64 wheels, so build each platform on (or emulating) that platform, or use pure-Python packages."
---

| Python | BuildKit |
| --- | --- |
| `~/.cache/pip` | `--mount=type=cache,target=/root/.cache/pip` |
| `PIP_INDEX_URL` with a token | `--mount=type=secret,id=pip_conf,target=/etc/pip.conf` |
| `manylinux_x86_64` vs `aarch64` wheels | `--platform linux/amd64,linux/arm64` |
| `__version__` | `ARG VERSION` and a label |
