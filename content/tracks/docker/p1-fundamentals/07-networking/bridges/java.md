---
from: java
status: draft
concept: "Service addresses and ports"
from_way: "A Spring Boot app reads spring.datasource.url=jdbc:postgresql://localhost:5432/app, because on a developer machine the database runs locally."
native_way: "In containers, the database is another network namespace: use its service name (jdbc:postgresql://db:5432/app) on a shared network, and let the app bind 0.0.0.0."
sensei_tip: "Spring Boot binds to all interfaces by default (server.address unset), so it works behind -p; a value of 127.0.0.1 there makes the published port dead."
---

| Java | Container networking |
| --- | --- |
| `localhost` in a JDBC URL | the container itself; use the service name |
| `server.port=8080` | the port inside; publish it with `-p 8080:8080` |
| `server.address` | leave unset (0.0.0.0) in containers |
| a service registry | Docker's embedded DNS on user-defined networks |
