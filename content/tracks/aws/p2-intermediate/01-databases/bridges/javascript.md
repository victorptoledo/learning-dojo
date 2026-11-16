---
from: javascript
status: draft
concept: "Pick the store from the access patterns"
from_way: "Prisma or Mongoose on one database, with queries written per page as needed."
native_way: "RDS or Aurora for relational data; DynamoDB for key-value access at scale, with a key per access pattern."
sensei_tip: "Use `@aws-sdk/lib-dynamodb` (`DynamoDBDocumentClient`) to read and write plain JS objects instead of `{S: ...}` attribute values."
---

| Node.js world | AWS |
| --- | --- |
| a MongoDB collection | a DynamoDB table (single-table design) |
| `findMany({ where })` | `QueryCommand` with a key condition |
| Prisma `$transaction` | `TransactWriteCommand` |
| a connection per Lambda | RDS Proxy or the RDS Data API |
