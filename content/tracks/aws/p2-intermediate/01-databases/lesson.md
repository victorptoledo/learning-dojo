# Databases: RDS, Aurora and DynamoDB

AWS offers a managed database for almost every data model. Two cover most backends: a
**relational** database (Amazon RDS or Aurora) and **DynamoDB**, a key-value and document
database built for predictable performance at any scale.

## RDS and Aurora

**Amazon RDS** runs PostgreSQL, MySQL, MariaDB, SQL Server, Oracle and Db2 for you: AWS
installs, patches (in your maintenance window), backs up and replaces failed hardware. You
choose the instance class, the storage (gp3 or io2) and the network placement.

| Feature | What it does |
| --- | --- |
| **Multi-AZ** | a synchronous standby in another AZ; failover in about a minute with the same DNS name; the standby takes no reads (Multi-AZ *clusters* add two readable standbys) |
| **Read replicas** | asynchronous copies for read scaling, in the same or another region; can be promoted |
| **Automated backups** | daily snapshots plus transaction logs: **point-in-time recovery** to any second in the retention window (up to 35 days) |
| **RDS Proxy** | a connection pool in front of the database; essential for Lambda, which can open thousands of connections |
| **IAM authentication** and **Secrets Manager** | no password in config: `ManageMasterUserPassword` lets RDS create and rotate the secret |
| **Encryption** | at rest with KMS (chosen at creation), in transit with TLS |

**Aurora** is AWS's cloud-native MySQL- and PostgreSQL-compatible engine: storage is
replicated six ways across three AZs and grows by itself, up to 15 low-lag replicas share
it, failover takes seconds, and **Aurora Serverless v2** scales capacity in small steps.
**Aurora DSQL** goes further with a distributed, serverless PostgreSQL-compatible database
for active-active multi-region use.

Place databases in private data subnets, reachable only from the app's security group
(Phase 1), with deletion protection on and a final snapshot on delete.

## DynamoDB

DynamoDB stores **items** (up to 400 KB) in **tables**, identified by a **primary key**:

- a **partition key** alone (`pk`), or
- a partition key plus a **sort key** (`pk`, `sk`): items with the same `pk` form an
  **item collection**, stored together and sorted by `sk`.

There are no joins and no ad hoc queries. Every efficient read is a `GetItem` (one key) or
a `Query` (one partition key, with a condition on the sort key: `=`, `<`, `between`,
`begins_with`). A `Scan` reads the whole table: fine for a migration, wrong for a request.

So you design **from the access patterns**, often with several entity types in one table
(**single-table design**):

| Access pattern | pk | sk |
| --- | --- | --- |
| customer profile | `CUSTOMER#17` | `PROFILE` |
| customer's orders, newest first | `CUSTOMER#17` | `ORDER#20261115-0042` (query descending) |
| order with its lines | `ORDER#0042` | `LINE#001`, `LINE#002` |

A **global secondary index** (GSI) re-keys the table on other attributes (`gsi1pk =
STATUS#PLACED`) for patterns the base key cannot answer; it is updated asynchronously, so
its reads are eventually consistent. A **local secondary index** keeps the partition key
with another sort key and must be created with the table.

Partition keys must have **high cardinality**: all traffic for one key value goes to one
partition, which can serve about 3,000 reads and 1,000 writes per second. A key like
`DATE#today` makes a hot partition.

### Writes, transactions and change streams

- **Condition expressions** make writes safe: `attribute_not_exists(pk)` (create only),
  `#status = :placed` (a state machine), `version = :expected` (optimistic locking). A
  failed condition raises `ConditionalCheckFailedException` and changes nothing.
- **Update expressions** change attributes in place: `SET`, `REMOVE`, `ADD` (atomic
  counters), `DELETE` (from sets).
- **Transactions** (`TransactWriteItems`, up to 100 items) are all-or-nothing across items
  and tables, for twice the cost of normal writes.
- **DynamoDB Streams** emit every change for 24 hours to Lambda: update aggregates, fan out
  events, replicate to search. **TTL** deletes expired items for free.
- **Global tables** replicate a table across regions, active-active.

### Capacity and consistency

**On-demand** mode bills per request and absorbs spikes: the default for new and spiky
workloads. **Provisioned** mode with auto scaling is cheaper for steady traffic. Reads are
eventually consistent by default (half the cost); ask for `ConsistentRead=True` when you
must read your own write from the base table.

## Choosing

| Need | Choose |
| --- | --- |
| relational data, joins, ad hoc queries, reporting, existing SQL skills | RDS or Aurora PostgreSQL |
| known access patterns, any scale, single-digit-millisecond latency, serverless | DynamoDB |
| a cache or sessions | ElastiCache (Valkey, Redis OSS, Memcached) or DynamoDB with DAX |
| search | OpenSearch Service |
| analytics on large data | S3 with Athena, or Redshift |

Many systems use two: DynamoDB for the request path, and a stream into S3 or a relational
store for reporting.

## Summary

- RDS and Aurora run relational engines; Multi-AZ for failover, replicas for reads, PITR for mistakes.
- Lambda needs RDS Proxy; secrets live in Secrets Manager; databases sit in private subnets.
- DynamoDB keys come from access patterns; Query and GetItem, never Scan on a request path.
- GSIs answer other patterns; conditions, transactions, streams and TTL complete the toolbox.
- Choose by access patterns, query flexibility and scale, and do not fear using two stores.

## Further reading

- Amazon RDS user guide: <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Welcome.html>
- Multi-AZ deployments: <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Concepts.MultiAZ.html>
- Amazon Aurora user guide: <https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/CHAP_AuroraOverview.html>
- RDS Proxy: <https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-proxy.html>
- DynamoDB core components: <https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.CoreComponents.html>
- Best practices for designing with DynamoDB: <https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html>
- Condition expressions: <https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ConditionExpressions.html>
