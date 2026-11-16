---
from: java
status: reviewed
concept: "Plug-ins for the database, and sharing its connections"
from_way: "Adding a Maven dependency for a feature, and a HikariCP pool per JVM sized at the default of 10, or larger to be safe."
native_way: "CREATE EXTENSION for features inside the database, small pools per instance, and a pooler such as PgBouncer in front of the server."
sensei_tip: "HikariCP's own guidance is a small pool: about cores × 2 per database server, shared by all instances. Behind PgBouncer in transaction mode, set prepareThreshold=0 on the JDBC driver unless PgBouncer is 1.21+ with max_prepared_statements."
---

| Java | PostgreSQL |
| --- | --- |
| a Maven dependency | CREATE EXTENSION |
| a Java agent loaded with -javaagent | shared_preload_libraries (restart) |
| HikariCP maximumPoolSize | connections from one instance |
| a shared pool service for many JVMs | PgBouncer |
| ThreadLocal state on a pooled connection | session state, unsafe in transaction mode |
