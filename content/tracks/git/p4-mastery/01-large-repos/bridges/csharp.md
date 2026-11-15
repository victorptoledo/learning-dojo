---
from: csharp
status: draft
concept: "Large repositories"
from_way: "TFVC workspaces that map only some server folders, and game assets checked in with exclusive locks."
native_way: "Sparse checkout replaces workspace mappings; Git LFS (with file locking) replaces exclusive check-outs for binaries."
sensei_tip: "For binary assets two people must not edit at once (Unity scenes, PSDs), git lfs lock FILE gives you the TFVC-style exclusive lock on hosts that support it."
---

| TFVC | Git |
| --- | --- |
| Workspace mappings / cloaking | `git sparse-checkout` |
| Exclusive check-out of a binary | `git lfs lock` |
| Server-side history only | partial clone fetches blobs on demand |
| Destroy (remove history) | `git filter-repo` and a re-clone |
