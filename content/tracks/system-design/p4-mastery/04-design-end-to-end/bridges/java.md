---
from: java
status: draft
concept: "From components to a whole system"
from_way: "Designing one Spring Boot service well: layers, transactions, tests."
native_way: "Designing the system around it: which service owns what data, which calls are synchronous, what is queued, cached or strongly consistent, and how it fails."
sensei_tip: "A service you can run with ./mvnw spring-boot:run is a component; a system design says how 50 of them behave when one is slow. Write an ADR (a Markdown file in docs/adr) for each choice that cost you a day to make."
---

| Java habit | End-to-end design habit |
| --- | --- |
| a class diagram | a request-path diagram with latencies |
| `@Transactional` across repositories | one strongly consistent step, the rest async |
| unit and integration tests | load tests and failure drills against SLOs |
| a README per service | ADRs for the system's decisions |
| `application.yml` profiles | regional and cell configuration |
