---
from: java
status: draft
concept: "Semantic API design"
from_way: "Endpoints like `/getUser` or `/deleteProduct` called with POST, ignoring HTTP verbs."
native_way: "Resources are plural nouns (`/users/123`); HTTP verbs (`GET`, `POST`, `PUT`, `DELETE`) carry the action."
sensei_tip: "Use `PUT` for full, idempotent replacement and `PATCH` for partial updates."
---
