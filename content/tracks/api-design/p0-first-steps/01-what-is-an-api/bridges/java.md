---
from: java
status: draft
concept: "The API as a contract"
from_way: "Returning JPA entities straight from `@RestController` methods, so the JSON is whatever the entity happens to contain."
native_way: "A separate DTO (often a `record`) per representation, designed for the consumer; the entity stays behind the controller."
sensei_tip: "Jackson serialises every getter: adding a `getInternalNotes()` to an entity silently adds a public field. Records as DTOs make the contract explicit."
---

| Java / Spring | API concept |
| --- | --- |
| `@Entity Order` returned as JSON | implementation leaking into the contract |
| `record OrderResponse(...)` | an explicit representation |
| springdoc-openapi or a hand-written `openapi.yaml` | the written contract |
| `@JsonIgnore` sprinkled on entities | a sign the contract is not designed |
