---
from: java
status: draft
concept: "Pick the store from the access patterns"
from_way: "Every service uses JPA on one shared PostgreSQL schema, and new queries are added as they come."
native_way: "Relational data goes to RDS or Aurora; key-based, high-scale access goes to DynamoDB with keys designed from the queries."
sensei_tip: "Use the DynamoDB Enhanced Client (`@DynamoDbBean`, `TableSchema.fromBean`) for item mapping, but design the keys first; and use the AWS JDBC Wrapper with Aurora for fast failover."
---

| Java world | AWS |
| --- | --- |
| `@Entity` with `@OneToMany` | an item collection: one pk, many sk |
| `@Version` optimistic locking | a condition expression on a version attribute |
| HikariCP pool per instance | RDS Proxy in front of the database |
| `@Transactional` | `TransactWriteItems`, up to 100 items |
