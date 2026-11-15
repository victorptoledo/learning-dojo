---
from: python
status: draft
concept: "How data is stored and found"
from_way: "Django models or SQLAlchemy tables, with pandas for analytics on the same database."
native_way: "Separate the transactional store (row-oriented, indexed by key) from analytics (columnar, fed by a stream), and put files in object storage."
sensei_tip: "pandas reading a Parquet file is your first contact with columnar storage: it reads only the columns you ask for. That is why warehouses scan billions of rows quickly."
---

| Python | Storage concept |
| --- | --- |
| `models.Index(fields=["customer", "created_at"])` | a composite index |
| `sqlite3` | a B-tree engine with a WAL mode |
| `pyarrow.parquet` | columnar, compressed analytics files |
| `boto3` `put_object` | object storage for blobs |
| `pybloom-live` / `rbloom` | Bloom filters |
