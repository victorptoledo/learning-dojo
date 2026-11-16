---
from: java
status: reviewed
concept: "Owning the whole request, not one tier"
from_way: "A Spring Boot developer often stops at the controller: the browser, the CDN and the deploy belong to other people."
native_way: "The full stack developer follows the request from the browser's network tab through the proxy and Spring down to the SQL Hibernate runs, and back."
sensei_tip: "Turn on SQL logging and open the browser's network tab side by side: most of your first full stack bugs are visible in one of the two."
---

| Java habit | End to end |
| --- | --- |
| `@RestController` is the edge | the browser, DNS, TLS and a CDN come first |
| a stack trace in the log | the browser console, the network tab and the trace id |
| `@Transactional` | constraints in the schema still protect the data |
| one WAR or JAR | an image plus a static front-end bundle plus migrations |
