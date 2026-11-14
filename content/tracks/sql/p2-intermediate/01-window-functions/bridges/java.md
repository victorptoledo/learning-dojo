---
from: java
status: draft
concept: "Relational calculation without collapsing rows"
from_way: "Loading thousands of rows into the application to compute moving averages or rankings in loops."
native_way: "`ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC)` computes the ranking inside the database engine."
sensei_tip: "Window functions turn problems that needed many lines of backend code into one clear query."
---
