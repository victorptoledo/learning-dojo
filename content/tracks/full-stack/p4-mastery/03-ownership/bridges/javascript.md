---
from: javascript
status: reviewed
concept: "Old bundles are still running"
from_way: "Shipping an API change and a front-end change together and assuming every user gets both."
native_way: "Users keep old tabs open for days: the API supports the old and the new shape during the transition, flags switch the UI gradually, and removal waits for metrics."
sensei_tip: "Log the front-end version (a build id header) on every API call, and you can tell when nobody uses the old shape any more."
---

| JavaScript | Production ownership |
| --- | --- |
| deploy front end and API together | support old clients during the change |
| `if (newCheckout)` hard-coded | an OpenFeature flag with an owner and expiry |
| Prisma migration that drops a column | expand and contract over releases |
| errors after a deploy | roll back the artifact or switch off the flag |
