---
from: java
status: draft
concept: "Beyond the framework"
from_way: "Thinking of the backend as the controller, service and repository you write, with the server, network and database as someone else's concern."
native_way: "Owning the whole path of a request: DNS, TLS, load balancer, application, database, other services and telemetry, and the cost of each step."
sensei_tip: "When an endpoint is slow, count its round trips (queries and remote calls) before profiling the Java code."
---

| Framework view | Role view |
| --- | --- |
| "The controller returns the order" | DNS → TLS → load balancer → filters → controller → service → pool → query → JSON |
| "It works on my machine" | Does it work with 10 instances, a slow database and a failed dependency? |
| "Hibernate handles it" | How many queries, which indexes, which isolation level? |
