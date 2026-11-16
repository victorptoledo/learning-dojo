---
from: java
status: draft
concept: "Keeping history of changing entities"
from_way: "JPA entities updated in place, with Hibernate Envers if someone asked for an audit trail."
native_way: "Type 2 rows with their own surrogate key and a half-open validity range, closed and opened by the load."
sensei_tip: "Envers' audit tables are close to type 2, but they record every technical change; a type 2 dimension tracks only the attributes reports care about, keyed for star joins."
---

| Java | Slowly changing dimensions |
| --- | --- |
| entity.setName(...) and save | type 1 |
| Hibernate Envers revision tables | close to type 2 history |
| @Version optimistic lock | not history: concurrency control |
| Objects.equals(a, b) with nulls | IS DISTINCT FROM |
| LocalDate validFrom, validTo | the validity range |
