---
from: java
status: reviewed
concept: "What sits behind a JDBC connection"
from_way: "A HikariCP pool of java.sql.Connection objects, treated as cheap handles, and JPA metadata from annotations."
native_way: "Each connection is a server process with its own memory; the database describes itself in the pg_catalog tables."
sensei_tip: "Size Hikari's maximumPoolSize from the database's point of view: instances × pool size must stay well under max_connections. Ten pods × 50 connections is 500 processes."
---

| Java | PostgreSQL |
| --- | --- |
| DriverManager.getConnection(url) | the postmaster forks a backend process |
| DatabaseMetaData.getTables(...) | SELECT ... FROM pg_class JOIN pg_namespace |
| jdbc:postgresql://host/shop | one connection, one database |
| currentSchema=billing in the URL | SET search_path = billing |
| JVM heap per process | work_mem per sort, per backend |
