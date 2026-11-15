---
from: javascript
status: draft
concept: "Security in ordinary code"
from_way: "Spreading req.body into a model and lookups by id alone."
native_way: "Explicit input schemas, ownership checks and limits."
sensei_tip: "Object.assign(user, req.body) lets a client set role or isAdmin; validate with a schema that only allows the editable fields."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| Object.assign(user, req.body) | pick allowed fields with a schema |
| findById(req.params.id) | findOne({ id, ownerId: req.user.id }) |
| express-rate-limit | rate limits |
| returning the whole document | a response DTO |
