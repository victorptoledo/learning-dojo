# Rate Limiting and Quotas

Every API has a capacity, and some client will eventually exceed it: a bug in a retry loop, a
scraper, a customer's migration script, a real attack. **Rate limits** protect the service and
make sharing it fair; **quotas** are the commercial version (requests per month per plan). For
the API designer the questions are what to limit, how to tell clients about it, and how clients
should react.

> The Backend track's "Rate Limiter" kata (`backend.p2.rate-limiter`) implements the
> algorithms; System Design's overload lesson (`system-design.p2.overload-backpressure`) covers load shedding. This
> lesson is about the contract.

## What to limit, and by what key

| Key | Example | Notes |
| --- | --- | --- |
| API key / OAuth client | 1,000 requests per minute per key | the usual primary limit |
| user (token subject) | 100 per minute per user | fairness inside one client app |
| IP address | for unauthenticated endpoints (login, sign-up) | weak behind NAT and proxies |
| endpoint or cost | expensive searches or exports count more | "cost-based" limits, as GitHub's GraphQL API does with points |
| concurrency | at most 10 requests in flight | protects slow endpoints better than a rate |

Use several layers: a generous global limit at the gateway, tighter ones on expensive or
abusable endpoints, separate ones for writes.

## Algorithms in one table

| Algorithm | Behaviour | Trade-off |
| --- | --- | --- |
| Fixed window | N per calendar minute | simple; allows 2N around the window edge |
| Sliding window (log or counter) | N in any 60-second span | smoother; a bit more state |
| Token bucket | a bucket of capacity B refilled at R per second; each request takes a token | allows short bursts up to B while enforcing the average R; the most common |
| Leaky bucket / GCRA | requests leave at a constant rate | smooth output; queueing adds latency |

In a fleet of servers the counters must be shared (Redis, a gateway) or the limit is per
instance.

## Telling the client: 429 and headers

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 17
RateLimit-Policy: "per-minute";q=1000;w=60
RateLimit: "per-minute";r=0;t=17
Content-Type: application/problem+json

{"type": "https://api.shop.example/problems/rate-limited", "title": "Too many requests",
 "status": 429, "detail": "This API key may send 1,000 requests per minute."}
```

- `429 Too Many Requests` (RFC 6585) with `Retry-After` (seconds or an HTTP date) is the
  essential part.
- The IETF HTTPAPI draft "RateLimit header fields" standardises `RateLimit-Policy` (the
  quota `q` per window `w`) and `RateLimit` (remaining `r`, seconds until reset `t`). Many
  APIs still send the older `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`.
- Send the headers on **successful** responses too, so well-behaved clients can slow down
  before they hit the wall.
- Use `503` with `Retry-After` when the **service** is overloaded for everybody, `429` when
  **this client** exceeded its share.

## What good clients do

- Honour `Retry-After`; otherwise back off exponentially with jitter.
- Watch the remaining budget and spread work (a nightly sync does not need 1,000 requests in
  the first second).
- Use bulk endpoints, webhooks instead of polling (lesson 5), and conditional requests
  (`304`s are cheap; some APIs, GitHub among them, do not count them against the limit).

## Designing the policy

- Publish limits per plan in the docs; make them visible per key in a dashboard.
- Prefer limits that degrade gracefully (reject the expensive endpoint, keep reads working).
- Exempt health checks, and never rate-limit an idempotent retry of a request that is still in
  progress more harshly than the original.
- Alert on clients that live at their limit: they often need a bulk endpoint or a conversation.

## Further reading

- RFC 6585, section 4, "429 Too Many Requests": <https://www.rfc-editor.org/rfc/rfc6585#section-4>
- IETF HTTPAPI draft, "RateLimit header fields for HTTP": <https://datatracker.ietf.org/doc/draft-ietf-httpapi-ratelimit-headers/>
- RFC 9110, section 10.2.3, "Retry-After": <https://www.rfc-editor.org/rfc/rfc9110#section-10.2.3>
- GitHub REST API docs, "Rate limits for the REST API": <https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api>
- Stripe engineering blog, "Scaling your API with rate limiters": <https://stripe.com/blog/rate-limiters>
- Cloudflare blog, "How we built rate limiting capable of scaling to millions of domains": <https://blog.cloudflare.com/counting-things-a-lot-of-different-things/>
