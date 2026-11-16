---
from: go
status: draft
concept: "Pick the store from the access patterns"
from_way: "A Go service uses `database/sql` with hand-written queries on PostgreSQL."
native_way: "RDS or Aurora with pgx for relational data; DynamoDB with `feature/dynamodb/attributevalue` for key-based access."
sensei_tip: "Use `attributevalue.MarshalMap` with `dynamodbav` struct tags, and `expression.NewBuilder()` to build condition and key expressions without string typos."
---

| Go world | AWS |
| --- | --- |
| `db.QueryContext` | `client.Query` with a key condition |
| `sql.Tx` | `TransactWriteItems` |
| `json` struct tags | `dynamodbav` struct tags |
| pgxpool | pgxpool plus RDS Proxy for serverless callers |
