---
from: python
status: draft
concept: "Tables, types and keys versus classes, fields and references"
from_way: "A dataclass or Pydantic model with type hints, and Decimal for money."
native_way: "A table whose column types are enforced on every write, not just hinted."
sensei_tip: "Python's float has the same rounding problem as SQL's double precision: use Decimal in code and numeric in the database."
---

| Python | SQL |
| --- | --- |
| @dataclass class Customer: id: int | CREATE TABLE customers (id bigint PRIMARY KEY, ...) |
| Decimal('9.50') | numeric(10, 2) |
| datetime.now(timezone.utc) | now() into a timestamptz |
| Optional[str] | a nullable column (no NOT NULL) |
