# API Failure Modes in Production

The rules of this track exist because each of them was learned the hard way somewhere. This
lesson collects the failure modes that keep recurring in API incidents and post-mortems, what
they look like from the outside, and which design decision prevents them. Use it as a
pre-launch checklist and as a guide when something goes wrong.

## 1. The accidental breaking change

*Symptom:* after a harmless-looking deploy, one partner's integration fails; your own tests
are green.
*Typical causes:* a field became `null` instead of absent, an enum gained a value, a default
sort changed, validation got stricter, a serializer upgrade changed date formats, a field
renamed by a refactoring tool.
*Prevention:* contract tests per consumer, an OpenAPI diff in CI, tolerant-reader guidance in
the docs, serialisation configured explicitly (not by library defaults), canary releases
watched by error rate **per client**.

## 2. Retry storms

*Symptom:* a short database hiccup becomes a 40-minute outage. Traffic triples while the
service is down.
*Cause:* every layer retries (SDK × gateway × service mesh × job runner), immediately and
without jitter; timeouts upstream are longer than downstream so abandoned requests keep working.
*Prevention:* retry budgets (retries at most 10% of traffic), exponential backoff with jitter,
only one layer retrying, deadlines that shrink downstream, `Retry-After` on `429`/`503`,
load shedding and circuit breakers (System Design phase 2).

## 3. Duplicate side effects

*Symptom:* customers charged twice, two orders shipped, a webhook processed twice.
*Cause:* `POST` retried after a timeout; at-least-once message delivery with a non-idempotent
consumer.
*Prevention:* idempotency keys with atomic checks (phase 2), idempotent webhook receivers,
unique constraints on natural keys.

## 4. Unbounded responses and expensive queries

*Symptom:* one client's request takes the database down; memory spikes on the API servers.
*Cause:* a collection without pagination, `limit=100000` accepted, offset paging at depth, a
filter on an unindexed column, a GraphQL query nested ten levels deep.
*Prevention:* default and maximum page sizes, cursors, filters only on indexed fields, query
cost limits, timeouts on database statements.

## 5. Pagination drift

*Symptom:* "we are missing records" or "we imported some twice" from clients that sync
nightly.
*Cause:* offset pagination over a changing collection; a sort key without a tie-breaker.
*Prevention:* keyset cursors on a unique key; for sync use cases, a change feed
(`updatedSince` plus cursor) or events.

## 6. The thundering herd

*Symptom:* traffic spikes at every full hour, or right after an outage ends, or when a cache
entry expires.
*Cause:* clients scheduled with cron at `:00`, synchronised reconnects, many caches expiring at
once.
*Prevention:* jitter in client schedules and backoff, `stale-while-revalidate`, request
coalescing at the cache, rate limits per client.

## 7. Leaky errors and broken object-level authorisation

*Symptom:* a security report: stack traces with host names in `500` bodies, or another
customer's invoice readable by changing an id in the URL.
*Prevention:* one error handler that maps everything to problem details, generic `500`s with
a correlation id, an ownership check on every object lookup, opaque ids, tests that try other
tenants' ids (OWASP API Security Top 10).

## 8. Deprecations nobody noticed

*Symptom:* sunset day comes, an important customer breaks, and the old version is switched
back on for "just six more months", forever.
*Prevention:* usage dashboards per API key, `Deprecation` and `Sunset` headers from day one
of the deprecation, direct contact with the remaining consumers, brownouts before the
switch-off.

## 9. Clock, time zone and precision bugs

*Symptom:* events "from the future", reports off by one day, money off by a cent.
*Prevention:* RFC 3339 timestamps in UTC, explicit precision, money as integers or decimal
strings, server-generated timestamps for ordering.

## Operating an API

- **Observability per consumer and per operation**: request rate, error rate by status class
  and problem type, latency percentiles; logs with a correlation id that also appears in
  problem details.
- **SLOs** for the API (for example 99.9 % of requests succeed, p99 under 500 ms) and an error
  budget that decides when to stop shipping features (`backend.p3.sre-observability`).
- **A status page and a changelog**, so consumers learn about incidents and changes from you.
- **Blameless post-mortems** whose action items often become new style-guide rules or lint
  checks: governance learns from production.

## Further reading

- AWS Builders' Library, "Timeouts, retries, and backoff with jitter": <https://aws.amazon.com/builders-library/timeouts-retries-and-backoff-with-jitter/>
- AWS Builders' Library, "Avoiding insurmountable queue backlogs": <https://aws.amazon.com/builders-library/avoiding-insurmountable-queue-backlogs/>
- Google SRE Book, "Handling Overload" and "Addressing Cascading Failures": <https://sre.google/sre-book/addressing-cascading-failures/>
- OWASP API Security Top 10 (2023): <https://owasp.org/API-Security/editions/2023/en/0x11-t10/>
- Google AIP-180, "Backwards compatibility": <https://google.aip.dev/180>
- Stripe engineering blog, "Designing robust and predictable APIs with idempotency": <https://stripe.com/blog/idempotency>
