---
from: python
status: draft
concept: "Asking a database for data instead of looping over it"
from_way: "A list comprehension over objects, or pandas filtering a DataFrame in memory."
native_way: "One declarative SELECT run where the data lives, without moving every row into the process first."
sensei_tip: "pandas and SQL share many ideas (filter, group, join), but SQL scales past your RAM and is shared by every service that uses the database."
---

| Python | SQL |
| --- | --- |
| [c for c in customers if c.country == 'PT'] | SELECT * FROM customers WHERE country = 'PT' |
| sorted(rows, key=lambda r: r.name) | ORDER BY name |
| psycopg.connect(...) | psql -d dojo |
| df[df.price < 20] | WHERE price < 20 |
