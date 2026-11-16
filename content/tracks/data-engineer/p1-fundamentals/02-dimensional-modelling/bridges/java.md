---
from: java
status: draft
concept: "Modelling for reading, not writing"
from_way: "JPA entities in third normal form, with @OneToMany relationships designed for safe updates."
native_way: "A star schema: a fact table of events with foreign keys to wide, descriptive dimension tables."
sensei_tip: "Do not map a star schema to a rich entity graph. Analytical queries are set-based: one SELECT joining the fact to its dimensions, grouped by dimension attributes."
---

| Java | Dimensional modelling |
| --- | --- |
| @Entity Order with @OneToMany lines | fact_sales at order-line grain |
| @Id Long id from a sequence | a surrogate key |
| a business key (email, SKU) | the natural key in the dimension |
| an enum field | a dimension attribute |
| BigDecimal amount | an additive measure |
