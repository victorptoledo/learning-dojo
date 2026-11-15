---
from: java
status: draft
concept: "Storing each fact once"
from_way: "Class design: one class per concept, references between objects, and avoiding duplicated fields."
native_way: "Tables in third normal form: each fact once, related by keys."
sensei_tip: "The same instinct that makes you extract a class from duplicated fields makes you extract a table. A @ManyToMany becomes a junction table; give it its own entity when it gains attributes."
---

| Java | SQL |
| --- | --- |
| duplicated fields in two classes | an update anomaly waiting to happen |
| class Customer referenced by Order | customers table, orders.customer_id |
| @ManyToMany Set<Course> courses | enrolments (student_id, course_id) |
| List<String> tags in one String | a 1NF violation: use a tags table |
| a snapshot copy of price in OrderLine | deliberate denormalisation |
