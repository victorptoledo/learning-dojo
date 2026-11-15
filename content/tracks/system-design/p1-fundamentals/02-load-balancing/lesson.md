# Load Balancers (L4 vs L7) and Consistent Hashing

A load balancer turns many servers into one service: it spreads requests, removes servers that
fail, and lets you deploy, scale and drain machines without users noticing. When the servers
hold **state** (a cache, a partition of data), the question changes from "which server is
free?" to "which server owns this key?", and that is where **consistent hashing** comes in.

## Layer 4 and layer 7

| | L4 (transport) | L7 (application) |
| --- | --- | --- |
| Sees | IP addresses, ports, TCP/UDP | HTTP method, path, headers, cookies, gRPC service |
| Decides | per connection | per request |
| Can | forward packets or proxy connections at very high rates | route `/api` and `/img` to different services, retry, rewrite, add headers |
| TLS | passes it through (or terminates) | terminates it to read the request |
| Examples | AWS NLB, Linux IPVS, Maglev | AWS ALB, NGINX, Envoy, HAProxy in HTTP mode |
| Cost | cheap, low latency | more CPU per request, much more flexible |

A common layout uses both: an L4 tier spreads connections over a fleet of L7 proxies, which
route requests to services. HTTP/2 and gRPC multiplex many requests over one long-lived
connection, so an L4 balancer would pin all of them to one backend: you need L7 (per request)
balancing for them.

## Algorithms

| Algorithm | How it picks | Good for |
| --- | --- | --- |
| Round robin | next server in turn | identical servers, similar requests |
| Weighted round robin | in proportion to weights | mixed hardware, canaries |
| Least connections / least outstanding requests | the server with the fewest in flight | uneven request costs |
| Power of two choices | pick two at random, send to the less loaded | large fleets; near-optimal, no global state |
| Random | uniform random | simple, surprisingly fine at scale |
| Hash (IP, header, key) | `hash(key) mod N` or a ring | stickiness, caches, partitioned data |

**Least outstanding requests** reacts to a slow server; round robin keeps feeding it. **Power
of two choices** gets most of that benefit with no shared counter between balancer instances,
which is why large proxies (Envoy, NGINX's `random two least_conn`) use it.

## Health checks and draining

- **Active checks**: the balancer calls `GET /health` every few seconds; after N failures it
  stops sending traffic, after M successes it adds the server back.
- **Passive checks (outlier detection)**: it watches real responses and ejects a server that
  returns many 5xx or times out.
- A health endpoint should say whether **this instance** can serve. If it also checks the
  shared database, a database blip marks *every* server unhealthy at once and the balancer has
  nothing left: a self-inflicted outage.
- **Connection draining** (deregistration delay): on deploy, stop sending new requests to the
  instance, let in-flight ones finish, then stop it.

Stateless servers make all of this easy. Keep sessions in a shared store or a signed token,
not in server memory; "sticky sessions" are a crutch that breaks on every deploy and failure.

## When servers own keys: hashing

A cache tier of N servers needs each key to land on the same server every time. The naive
answer, `server = hash(key) mod N`, breaks badly when N changes:

```text
N = 4 → 5: a key stays put only if hash mod 4 == hash mod 5 → about 1 in 5 keys
           so ~80 % of the cache moves (and misses) when you add one server
```

### Consistent hashing

Place servers and keys on the same circle of hash values (a **ring**). A key belongs to the
first server clockwise from its position.

```text
            0
      S3 ●     ● S1          key k1 hashes here → walks clockwise → S1
          \   /
  k2 ×     ring    × k1
          /   \
      S2 ●     ●  (S1's second virtual node)
```

- Adding a server takes over only the arc before it: about **1/N** of the keys move, all of them
  to the new server.
- Removing a server hands its arcs to the next servers clockwise; nothing else moves.
- **Virtual nodes**: each server is placed at many points (100–200 is common, hashing
  `"S1#0"`, `"S1#1"`, ...). That evens out the arcs, and when a server leaves its load spreads
  over many neighbours instead of doubling one. Weights become "more virtual nodes".
- **Replication**: walk clockwise and take the first R *distinct* servers as the replicas of a
  key (Dynamo and Cassandra do this).

Alternatives with the same goal: **rendezvous (highest random weight) hashing** scores every
server for a key and picks the highest, with no ring to store; **jump consistent hash** maps a
key to a bucket number with no memory at all, but only supports adding or removing the last
bucket; Google's **Maglev** builds a lookup table for L4 balancers. Many databases instead use
a **fixed number of partitions** mapped to nodes, which phase 3 covers.

## Where balancing happens

| Place | Example | Notes |
| --- | --- | --- |
| DNS | geo or weighted records | coarse, cached, region-level |
| Edge / CDN | anycast PoPs | TLS near the user |
| Dedicated balancer | ALB, NLB, HAProxy, NGINX | the classic box in the diagram |
| Sidecar / service mesh | Envoy next to each service | per request, with retries and mTLS |
| Client-side | gRPC client with a resolver, a Redis Cluster client | no extra hop; the client must know the servers |

A single balancer is itself a single point of failure: run them in pairs (or as a managed,
multi-zone service) and spread them across availability zones.

## Summary

- L4 balances connections cheaply; L7 balances requests and can route by content.
- Least outstanding requests and power of two choices handle uneven, slow servers.
- Health checks should test the instance, not shared dependencies; drain before stopping.
- `hash mod N` reshuffles almost every key when N changes; a consistent hash ring moves ~1/N.
- Virtual nodes even out the load; the first R distinct nodes clockwise are the replicas.

## Further reading

- Google SRE book, "Load Balancing in the Datacenter": <https://sre.google/sre-book/load-balancing-datacenter/>
- NGINX, "Using nginx as HTTP load balancer": <https://nginx.org/en/docs/http/load_balancing.html>
- DeCandia et al., "Dynamo: Amazon's Highly Available Key-value Store" (consistent hashing with virtual nodes): <https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf>
- Lamping and Veach, "A Fast, Minimal Memory, Consistent Hash Algorithm" (jump hash): <https://arxiv.org/abs/1406.2294>
- Wikipedia, "Consistent hashing": <https://en.wikipedia.org/wiki/Consistent_hashing>
