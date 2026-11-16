# What an API Is: a Contract With People You Will Never Meet

An **API** (application programming interface) is the part of a system that other programs are
allowed to depend on. For a web API that means the URLs, methods, headers, bodies, status
codes and behaviour that a client can observe. Everything behind it (classes, tables, queues)
is implementation and can change freely; everything in it is a **contract**, and changing it
can break somebody's code at three in the morning.

## Why API design is its own discipline

Code inside one service is cheap to change: you rename a method, the compiler finds every
caller, you fix them in one commit. An API is different:

- **You do not control the callers.** Mobile apps stay installed for years, partners deploy on
  their own schedule, scripts written by a customer's intern keep running after the intern left.
- **Mistakes are permanent.** Once a field name, an error code or a default page size is in
  production, someone relies on it. *Hyrum's law*: with enough users, every observable
  behaviour of your API will be depended on by somebody, documented or not.
- **The network is in the middle.** Calls are slow, fail halfway, are retried and arrive twice.
  A good API is designed for that (phase 2 of this track).
- **Your API is a product.** Its users are developers; their experience (how fast they get the
  first successful call, how clear the errors are) decides whether they adopt it.

## Consumers and providers

| Role | Cares about |
| --- | --- |
| **Provider** (you) | evolving the implementation, protecting the system (auth, limits), operating it |
| **Consumer** (client developers) | predictability, clear errors, stability, good docs, not being broken |

Design from the consumer side first. A useful exercise: write the client code you would like to
write **before** the server exists. That is the idea behind **API-first** (or design-first)
development: the contract (an OpenAPI document, phase 3) is written, reviewed and agreed, and
only then implemented. The alternative, *code-first*, generates the contract from the
implementation, which is fast but tends to leak internals (database column names, Java enum
quirks) into the public surface.

## API styles

| Style | Shape | Typical use |
| --- | --- | --- |
| **REST** over HTTP | resources (`/orders/42`) and the uniform HTTP methods | public and partner APIs, CRUD-heavy domains |
| **RPC** (gRPC, JSON-RPC) | named procedures (`CreateOrder`) with typed messages | internal service-to-service calls, streaming |
| **GraphQL** | one endpoint, clients ask for the exact fields they need | client-driven UIs with many screens |
| **Events / async** (webhooks, Kafka, AsyncAPI) | the provider pushes what happened | integrations, decoupled systems |

None is "the best". This track spends most of its time on HTTP APIs in the REST style because
they are the most common public interface and because HTTP itself teaches the hardest lessons
(caching, idempotency, status codes). Phase 3 compares the other styles.

## What makes an API good

1. **Consistent**: the same concept has the same name, shape and behaviour everywhere. Users
   learn one endpoint and can guess the next.
2. **Hard to misuse**: safe defaults, required fields really required, dangerous operations
   explicit.
3. **Evolvable**: designed so that new features are additive (phase 2).
4. **Honest about failure**: status codes and machine-readable errors that say what went wrong
   and what to do about it.
5. **Documented by a contract**: what is promised is written down, and tests check it.

> [!TIP]
> When in doubt, follow published guidelines instead of inventing: Google's API Improvement
> Proposals (AIP), the Microsoft REST API Guidelines and Zalando's RESTful API Guidelines
> answer most "how should we name/paginate/version this?" questions, and they mostly agree.

## How this track connects to others

The Backend track's lesson "REST API Design" (`backend.p1.api-design`) gives a one-lesson tour of
the same ground; this track goes deep. The Spring Boot track (`spring-boot.p1.rest-controllers`)
shows how to write controllers; here the question is what those controllers should promise.

## Further reading

- RFC 9110, "HTTP Semantics": <https://www.rfc-editor.org/rfc/rfc9110>
- Google API Improvement Proposals, "AIP-1: AIP Purpose and Guidelines" and the index: <https://google.aip.dev/>
- Microsoft REST API Guidelines: <https://github.com/microsoft/api-guidelines>
- Zalando RESTful API and Event Guidelines: <https://opensource.zalando.com/restful-api-guidelines/>
- Hyrum's Law: <https://www.hyrumslaw.com/>
- roadmap.sh, API Design roadmap: <https://roadmap.sh/api-design>
