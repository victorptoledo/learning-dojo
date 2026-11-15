---
from: python
status: draft
concept: "Thinking in tables"
from_way: "Django models or SQLAlchemy classes, with the schema generated from code."
native_way: "Tables, keys, constraints and indexes designed for the data and the queries."
sensei_tip: "Let the database enforce rules: unique=True and CheckConstraint in Django become real constraints. Check the generated migration SQL and the indexes it creates."
---

| Python | In practice |
| --- | --- |
| ForeignKey(Author) | a foreign key column with an index |
| ManyToManyField | a join table |
| unique=True | UNIQUE constraint |
| Meta.indexes | indexes matching the WHERE and ORDER BY |
| validators in Python | CHECK constraints in the database |
