---
from: javascript
status: draft
concept: "Keeping history of changing entities"
from_way: "Updating a user document in place, maybe with an updatedAt field."
native_way: "Version rows with validity ranges, so facts keep the context they had."
sensei_tip: "Event-sourced front-end stores (Redux history) and type 2 dimensions share an idea: never lose the old state, append a new one."
---

| JavaScript / TypeScript | Slowly changing dimensions |
| --- | --- |
| Object.assign(user, changes) | type 1 |
| push a new version, close the old | type 2 |
| previousRegion field | type 3 |
| a === b with null/undefined | careful: IS DISTINCT FROM semantics |
| findLast(v => v.validFrom <= date) | a point-in-time lookup |
