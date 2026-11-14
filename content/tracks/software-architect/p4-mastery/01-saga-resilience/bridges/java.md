---
from: java
status: draft
concept: "Distributed rollback"
from_way: "Distributed XA/2PC transactions that hold locks across the network."
native_way: "A saga is a sequence of local transactions; if step 3 fails, compensating transactions run (refund the payment, cancel the booking)."
sensei_tip: "Choreography (simple events) suits short flows across a few services; orchestration (a central state machine) suits longer, complex ones."
---
