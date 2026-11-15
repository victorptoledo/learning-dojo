# DNS, Anycast and Edge CDNs

Before a request reaches your code it has already been routed twice: by **DNS**, which turns a
name into an address, and often by a **CDN**, which answers from a machine near the user. Both
are the cheapest scaling and latency tools you have, and both are distributed caches with
their own staleness rules.

## How a name is resolved

```text
browser → OS stub resolver → recursive resolver (ISP, 1.1.1.1, 8.8.8.8)
                                   │ cache miss
                                   ├─► root server:        "ask the .com servers"
                                   ├─► .com TLD server:    "ask ns1.example.com"
                                   └─► authoritative NS:   "www.example.com A 203.0.113.10, TTL 300"
```

The **recursive resolver** does the walking and caches every answer for its **TTL** (time to
live, in seconds). The **authoritative** servers hold your zone. Every layer caches: the
browser, the operating system, the resolver.

| Record | Holds | Typical use |
| --- | --- | --- |
| `A` / `AAAA` | an IPv4 / IPv6 address | the entry point of a service |
| `CNAME` | another name (an alias) | `www` → `shop.cdn-provider.net` |
| `NS` | the name servers of a zone | delegation from the TLD |
| `MX` | mail servers with priorities | email |
| `TXT` | free text | domain verification, SPF, DKIM |
| `SRV` | host and port of a service | service discovery inside a network |

A `CNAME` cannot sit at the zone apex (`example.com` itself); providers offer an "alias" or
"ANAME" record that resolves the target on the server side.

## TTL is a trade-off

| Short TTL (30–60 s) | Long TTL (hours) |
| --- | --- |
| failover and migrations take effect quickly | fewer queries, faster for users (cache hits) |
| more load on the authoritative servers | a bad record stays cached for hours |
| more resolution latency for users | changes need planning: lower the TTL days before |

The classic migration: lower the TTL to 60 s a day in advance, switch the record, keep the old
servers running until the old TTL has expired everywhere, then raise the TTL again. Some
clients (old JVMs, some libraries) cache longer than the TTL, so never turn the old address off
the minute you switch.

## DNS as a load balancer

- **Round-robin DNS**: several `A` records; clients pick one. Cheap, but there are no health
  checks, and caching makes the split uneven.
- **Weighted** records send 10 % of users to a new region (a canary).
- **Latency-based or geo DNS**: the authoritative server answers with the region closest to
  the resolver (or to the client subnet, with the EDNS Client Subnet extension).
- **Health-checked failover**: the DNS provider stops returning an address whose health check
  fails. Recovery is bounded by the TTL plus client caching: minutes, not seconds.

DNS is a coarse tool: good for choosing a region, poor for spreading load between servers.

## Anycast

With **anycast**, the same IP address is announced over BGP from many locations; the Internet's
routing delivers each packet to the "closest" one (in routing terms). Public resolvers such as
`1.1.1.1` and `8.8.8.8`, the DNS root servers and most CDNs use it.

| Unicast | Anycast |
| --- | --- |
| one address, one place | one address, many places |
| failover needs a DNS change | a site that withdraws its route is skipped within seconds |
| a DDoS hits one site | a DDoS is spread over every site |

Anycast fits short, stateless exchanges (DNS over UDP) best. TCP works too, because routes
rarely change during a connection, and CDNs run it at huge scale.

## Content delivery networks

A CDN is a fleet of caching reverse proxies in **points of presence** (PoPs) close to users.
The user's TLS handshake ends at the PoP, a few milliseconds away instead of a hundred; on a
cache hit the **origin** (your servers) never sees the request.

```text
user (Lisbon) ──10 ms──► PoP Lisbon ── hit ──► response
                              │ miss
                              └──80 ms──► origin (us-east-1) ──► response, cached at the PoP
```

| Push CDN | Pull CDN |
| --- | --- |
| you upload files to the CDN ahead of time | the PoP fetches from the origin on the first miss |
| good for big, rarely changing files (releases, videos) | good for websites; nothing to manage |
| you manage what is stored and its lifetime | the first user after expiry pays the miss |

### What and how long to cache

The origin controls the CDN with standard **HTTP caching** headers (RFC 9111):

```http
Cache-Control: public, max-age=31536000, immutable        # app.3f9a1c.js: fingerprinted asset
Cache-Control: public, s-maxage=60, stale-while-revalidate=30   # an HTML page shared by everyone
Cache-Control: private, no-store                           # a page with the user's account data
ETag: "v42"                                                # lets caches revalidate with If-None-Match
Vary: Accept-Encoding                                      # one cached copy per encoding
```

- **Fingerprint** static assets (`app.3f9a1c.js`) and cache them forever: a new build has a new
  name, so you never need to invalidate.
- `s-maxage` applies to shared caches only (the CDN), `max-age` to browsers too.
- **Purge** (invalidate) by URL or by tag when content changes before it expires; purges take
  seconds to minutes to reach every PoP.
- Watch the **cache key**: a `Vary: Cookie` or an unused query parameter in the key splits the
  cache into millions of copies, and the hit ratio collapses.

### Beyond static files

CDNs also terminate TLS, absorb DDoS attacks, run a web application firewall, keep warm
connections to the origin, and run small functions at the edge (redirects, A/B routing,
authentication checks). An **origin shield** (one designated mid-tier PoP) collapses misses
from all PoPs into one request to the origin.

## Putting it together

A typical global entry point: geo or latency DNS picks a region, anycast brings the user to a
nearby PoP, the PoP serves static content and cached pages, and only dynamic requests travel
to a regional load balancer. The design questions to ask:

1. Which responses can be shared between users, and for how long?
2. How do we invalidate when the data changes (fingerprints, short TTLs, purges)?
3. What happens when the origin is down: can the CDN serve stale content (`stale-if-error`)?
4. How fast must a regional failover be, and is DNS TTL fast enough?

## Summary

- Resolution: stub → recursive resolver → root → TLD → authoritative, cached by TTL everywhere.
- Short TTLs give fast failover and more queries; lower them before a migration.
- DNS balances between regions; it is too coarse and too cached for server-level balancing.
- Anycast: one IP announced from many sites; routing picks the closest and spreads attacks.
- A CDN caches at PoPs near users; control it with `Cache-Control`, fingerprints and purges,
  and keep the cache key small.

## Further reading

- RFC 1034, "Domain Names: Concepts and Facilities": <https://www.rfc-editor.org/rfc/rfc1034>
- RFC 9111, "HTTP Caching": <https://www.rfc-editor.org/rfc/rfc9111>
- Cloudflare blog, "A Brief Primer on Anycast": <https://blog.cloudflare.com/a-brief-anycast-primer/>
- Google SRE book, "Load Balancing at the Frontend" (DNS and anycast): <https://sre.google/sre-book/load-balancing-frontend/>
- roadmap.sh, System Design (DNS and CDN topics): <https://roadmap.sh/system-design>
