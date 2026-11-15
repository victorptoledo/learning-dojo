---
from: java
status: draft
concept: "Asking a database for data instead of looping over it"
from_way: "A List of objects filtered with streams, or rows fetched over JDBC and processed in loops."
native_way: "One declarative SELECT: the database decides how to find the rows, with indexes and a planner."
sensei_tip: "Treat SQL as a second language you write deliberately, not as strings your ORM hides. Keep a psql or DBeaver window open next to your IDE."
---

| Java | SQL |
| --- | --- |
| list.stream().filter(c -> c.country().equals("PT")) | SELECT ... FROM customers WHERE country = 'PT' |
| .sorted(comparing(Customer::name)) | ORDER BY name |
| DriverManager.getConnection(url) | psql -d dojo, or a pool such as HikariCP |
| a JUnit test | a dojo test in tests/visible.sql |
