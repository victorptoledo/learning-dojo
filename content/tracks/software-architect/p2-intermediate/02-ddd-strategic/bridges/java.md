---
from: java
status: draft
concept: "Modelling boundaries"
from_way: "One giant `User` object with 40 columns shared by auth, billing, support and sales."
native_way: "Bounded contexts: in Sales the customer is a `Buyer`; in Billing it is a `BillingAccount`. Different models for different problems."
sensei_tip: "Never build a single canonical model for the whole company: it leads to a tightly bound distributed monolith."
---
