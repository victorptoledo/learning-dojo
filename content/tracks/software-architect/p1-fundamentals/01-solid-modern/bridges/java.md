---
from: java
status: draft
concept: "Object-oriented design principles"
from_way: "Creating `UserServiceImpl` for `UserService` only out of ceremony."
native_way: "Real dependency inversion: the core domain defines the interfaces it needs; external adapters (database, HTTP) implement them."
sensei_tip: "Coupling isn't about having references; it's about the cost of change. If a schema change breaks your business rules, your coupling is dangerous."
---
