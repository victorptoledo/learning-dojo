---
from: python
status: draft
concept: "Storing each fact once"
from_way: "Nested dicts and lists in JSON-like records, or a wide pandas DataFrame with repeated columns."
native_way: "Separate tables per entity and junction tables for many-to-many."
sensei_tip: "A flat DataFrame exported from a spreadsheet is a denormalised table; df.drop_duplicates on the entity's columns is how you would carve out its table."
---

| Python | SQL |
| --- | --- |
| {'order': 1, 'customer_name': 'Ada'} repeated | redundancy: move the name to customers |
| record['tags'] = ['a', 'b'] | a tags table with a foreign key |
| ManyToManyField | a junction table |
| df[['email', 'name']].drop_duplicates() | the customers table |
