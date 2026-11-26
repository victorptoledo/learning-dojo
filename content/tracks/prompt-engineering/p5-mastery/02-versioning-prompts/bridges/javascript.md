---
from: javascript
status: draft
concept: "Lock files and release channels"
from_way: "package.json with semver ranges, a lock file that pins exact versions, and release channels (canary, beta, stable)."
native_way: "Each prompt has a version that pins its template, model snapshot and parameters; new versions go out through shadow and canary stages and can be switched back by a flag."
sensei_tip: "A model alias is a caret range for your product's behaviour. Pin the snapshot like package-lock.json pins a version, and upgrade deliberately."
---

| JavaScript | Prompt-versioning concept |
| --- | --- |
| `package-lock.json` | pinned model snapshot and parameters |
| `"^2.0.0"` | a moving model alias (avoid) |
| canary and stable channels | canary rollout of a prompt version |
| a LaunchDarkly-style flag | selecting a version at run time |
| conventional commits | changelog entries per prompt change |
