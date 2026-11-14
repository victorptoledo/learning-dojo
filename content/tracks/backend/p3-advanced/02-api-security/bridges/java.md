---
from: java
status: draft
concept: "Security in ordinary code"
from_way: "Trusting the UI to hide admin buttons, binding requests onto entities, returning entities as JSON, and leaving actuator and stack traces open."
native_way: "Per-object ownership checks, deny-by-default roles, input and output records, limits on everything, allow-listed outbound calls and hardened configuration."
sensei_tip: "For every endpoint, write one test where a different user calls it with a valid id: if it does not answer 404 (or 403), you have a BOLA."
---

| Vulnerable | Safe |
| --- | --- |
| `repo.findById(id)` | `repo.findByIdAndOwnerId(id, user.id())` |
| `@RequestBody User user` | `@RequestBody UpdateProfile request` |
| `return userEntity;` | `return UserView.of(user);` |
| `restClient.get().uri(userUrl)` | allow-listed hosts, no private ranges |
