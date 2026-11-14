---
from: java
status: draft
concept: "Vertical vs horizontal data scaling"
from_way: "Buying a bigger database machine (scale-up) and hoping the connection pool copes."
native_way: "Send reads to replicas and writes to the primary; split huge tables horizontally (sharding by tenant_id or user_id)."
sensei_tip: "The biggest sharding risk is a bad shard key: it creates a hot partition that overloads one server while the others sit idle."
---
