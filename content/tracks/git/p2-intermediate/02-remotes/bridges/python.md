---
from: python
status: draft
concept: "Remotes and remote-tracking branches"
from_way: "pip install git+https://github.com/org/lib.git@main, which installs whatever main is at that moment."
native_way: "A remote-tracking branch is your snapshot of the server at the last fetch; a commit hash is the only thing that never moves."
sensei_tip: "Pin Git dependencies to a commit or tag (lib @ git+https://...@v1.4.0), never to a branch: branches are remote refs that move under you."
---

| Python | Git |
| --- | --- |
| `pip install git+URL@main` | fetch and use `origin/main` now |
| `@<sha>` in requirements | a fixed commit |
| PyPI mirror | another remote |
| `twine upload` | `git push` (for code, not packages) |
