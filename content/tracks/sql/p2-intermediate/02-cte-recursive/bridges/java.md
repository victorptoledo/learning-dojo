---
from: java
status: draft
concept: "CTEs and hierarchies"
from_way: "Unreadable subqueries nested five levels deep."
native_way: "`WITH RECURSIVE tree AS (SELECT ... UNION ALL SELECT ...)` walks parent-child hierarchies such as categories or org charts."
sensei_tip: "Since PostgreSQL 12, simple non-recursive CTEs are inlined by the planner, so readability costs nothing."
---
