# The Backend Role and the Life of a Request

A backend developer builds the part of a product users never see but always depend on: the
services that store data, enforce rules, talk to other systems, and stay up while traffic grows.
This track is about the **role**, the concepts that hold whatever language or framework you use.
The katas are in Java and Spring Boot, so take the Modern Java and Spring Boot tracks first.

## What the role owns

| Area | Questions you answer |
| --- | --- |
| **APIs** | What resources do clients see? What does each request mean and return? |
| **Data** | How is it modelled, stored, queried, kept consistent and migrated? |
| **Identity** | Who is calling, and what may they do? |
| **Integration** | How do we call other services and receive their events, reliably? |
| **Performance** | Is it fast enough? What happens at ten times the traffic? |
| **Reliability** | What happens when a dependency fails, a deploy goes wrong, a disk fills? |
| **Security** | What can an attacker do with this endpoint? |
| **Operations** | How do we know it works in production, and how do we fix it at 3 a.m.? |

Frameworks help with all of them, but they do not decide for you. A good backend developer knows
**what** the framework does underneath, so the right choices are made on purpose.

## The life of a request

Follow `GET https://shop.example.com/orders/42` from the browser to the database and back.

1. **DNS.** The client asks a resolver for the IP address of `shop.example.com`. The answer is
   cached for its TTL, which is why DNS changes take time to spread.
2. **TCP and TLS.** The client opens a connection to that address (port 443) and performs a TLS
   handshake: it checks the server's certificate and agrees on encryption keys. With HTTP/3 this
   happens over QUIC (UDP) in fewer round trips.
3. **Load balancer.** The address usually belongs to a load balancer or reverse proxy. It may end
   TLS, apply rate limits and routing rules, and forwards the request to one of several healthy
   instances of the service.
4. **The application.** The server parses the HTTP request; security filters authenticate the
   caller; the framework routes `GET /orders/{id}` to a controller method; validation runs.
5. **Business logic.** A service checks the rules (may this user see order 42?) and asks the data
   layer for what it needs.
6. **The database.** A connection is taken from the pool; the query runs; indexes decide whether
   that takes one millisecond or one second. Maybe a cache answers first.
7. **Other services.** The service may call another one (shipping status) over the network, with a
   timeout, or read an event it received earlier.
8. **The response.** The result is turned into JSON, with a status code and headers (content type,
   caching), and travels back through the load balancer to the client.
9. **Telemetry.** Along the way, logs, metrics and a trace record what happened and how long each
   step took.

Each step can fail, and each step has a lesson in this track.

## Latency numbers worth knowing

| Operation | Rough time |
| --- | --- |
| Reading from memory | ~100 nanoseconds |
| A simple indexed query in a local database | ~1 millisecond |
| A round trip within a data centre | ~0.5 millisecond |
| A query that scans a large table | 100 ms to seconds |
| A round trip between continents | ~150 milliseconds |
| A TLS handshake on a new connection | one or two extra round trips |

The lesson: network round trips and unindexed queries dominate. Doing ten sequential calls to
another service, or one query per item in a list, is how fast code becomes a slow API.

## Tools of the trade

- **An HTTP client** for exploring APIs: `curl`, HTTPie, the REST Client extension in VS Code.
  `curl -i` shows the status line and headers; `curl -v` shows the whole exchange.
- **A database client** to read query plans and data (psql, DBeaver, IntelliJ's database tools).
- **Containers** (Docker) to run databases, brokers and the service itself the same way everywhere.
- **Logs, metrics and traces** to see production.

```text
$ curl -i https://httpbin.org/get
HTTP/2 200
content-type: application/json
content-length: 254
...
```

## How the katas work here

Backend katas are Java or Spring Boot projects, checked in Docker with JUnit exactly like the
Spring Boot track. Some are plain Java (a JWT library, a rate limiter, a resilient client) because
the concept matters more than the framework; others are Spring APIs backed by an in-memory H2
database.

## Summary

- The backend role owns APIs, data, identity, integration, performance, reliability, security and
  operations.
- A request goes through DNS, TCP/TLS, a load balancer, the application, the database and other
  services, and comes back; every step can fail and has a cost.
- Round trips and unindexed queries dominate latency.
- `curl -i`, a database client, containers and telemetry are the daily tools.
