---
from: java
status: draft
concept: "Invariants and aggregates"
from_way: "Public setters (`order.setStatus(\"PAID\")`) that let any layer put an object in an invalid state."
native_way: "Immutable value objects (`Money`, `Email`); aggregates own their root and protect their rules through rich methods (`order.pay(payment)`)."
sensei_tip: "Keep aggregates small: each is the smallest unit of transactional consistency."
---
