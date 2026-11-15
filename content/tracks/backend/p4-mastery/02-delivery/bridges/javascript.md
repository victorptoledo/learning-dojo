---
from: javascript
status: draft
concept: "Shipping changes"
from_way: "npm scripts, CI pipelines and migrations with Prisma or knex at deploy."
native_way: "Continuous delivery with safe database changes and flags."
sensei_tip: "Prisma migrate generates a rename as drop plus add, which loses data; write the expand-and-contract steps yourself."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| prisma migrate deploy | expand-and-contract steps |
| big bang release | canary or progressive rollout |
| LaunchDarkly / Unleash | feature flags |
| revert the deploy | rollback compatible with the schema |
