---
from: javascript
status: draft
concept: "Promote one artefact; let the environment pull its state"
from_way: "A hosting platform deploys whatever is on main; promoting means moving a domain alias."
native_way: "The config repo says which image each environment runs; a merged pull request is the promotion and Argo CD syncs it."
sensei_tip: "Front-end builds often bake environment URLs in at build time; load them at run time instead, or you cannot promote one artefact."
---

| JavaScript world | GitOps |
| --- | --- |
| a preview deployment | an ephemeral environment |
| promote to production | a PR bumping the prod tag |
| instant rollback | a git revert |
| build-time `process.env` | run-time configuration |
