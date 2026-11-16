---
from: java
status: draft
concept: "Integration patterns and sagas"
from_way: "A chain of `RestTemplate` calls inside one `@Transactional` method, hoping the remote calls roll back with the database."
native_way: "Spring Integration or Apache Camel for the classic integration patterns, Kafka or RabbitMQ with dead letter topics, sagas orchestrated by a workflow engine (Temporal, Camunda) or choreographed with events, and Spring Cloud Gateway at the edge."
sensei_tip: "`@Transactional` stops at the JVM's database connection. A remote call inside it is not rolled back; that is exactly the gap a saga's compensations fill."
---

| Java | Integration idea |
| --- | --- |
| Apache Camel, Spring Integration | enterprise integration patterns as a DSL |
| `@RetryableTopic` and DLT in Spring Kafka | retries and dead letter channel |
| Temporal Java SDK, Camunda | orchestrated saga |
| events between services | choreographed saga |
| Spring Cloud Gateway | API gateway |
| a small Spring Boot app per client | backend for frontend |
