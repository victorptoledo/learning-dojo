---
from: python
status: draft
concept: "Pick the store from the access patterns"
from_way: "Django models with the ORM on PostgreSQL, adding indexes when a page is slow."
native_way: "RDS or Aurora for relational models; DynamoDB tables keyed for each access pattern, queried with boto3."
sensei_tip: "The boto3 `Table` resource converts Python types for you but needs `Decimal`, never `float`, for numbers; use `boto3.dynamodb.conditions.Key(\"pk\").eq(...)` to build key conditions."
---

| Python world | AWS |
| --- | --- |
| `Order.objects.filter(customer=c)` | `Query` on pk CUSTOMER#c |
| `select_for_update()` | a conditional write or transaction |
| `django.db.connection` per request | RDS Proxy for Lambda |
| a data migration | a backfill job writing new key attributes |
