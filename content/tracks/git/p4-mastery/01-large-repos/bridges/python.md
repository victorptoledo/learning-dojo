---
from: python
status: draft
concept: "Large repositories"
from_way: "Datasets, model weights and notebooks with embedded images committed next to the code."
native_way: "Code in Git; data and models in object storage or a tool built for it (DVC, LFS, a model registry), referenced by version."
sensei_tip: "A 2 GB checkpoint committed once makes every future clone 2 GB bigger. Add *.pt, *.ckpt and data/ to .gitignore on day one, and use DVC or LFS for what must be versioned."
---

| Python situation | Git technique |
| --- | --- |
| `model.pt` in the repository | LFS, DVC or a model registry |
| `data/*.csv` of several GB | not in Git; version it with DVC |
| notebooks with big outputs | strip outputs before committing |
| CI only runs the tests | `--depth 1` clone |
