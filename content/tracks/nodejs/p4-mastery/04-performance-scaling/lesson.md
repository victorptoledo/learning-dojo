# Performance, Profiling, Memory and Scaling

Node.js is fast enough for most backends, until a hot loop, a leak or one heavy endpoint makes
it slow for everyone. This lesson is the method: measure, profile, fix the real bottleneck,
then scale.

## Measure first

Optimise what you measured, not what you suspect.

- **Load test** with a realistic mix of requests: autocannon (Node.js), k6 or wrk.
  `npx autocannon -c 50 -d 30 http://localhost:3000/orders` reports throughput and latency
  percentiles.
- Look at **percentiles**, not averages: p50 is the typical request, p99 the worst one in a
  hundred, and users notice the tail. A blocked event loop shows up first in p99.
- Microbenchmarks (`performance.now()`, the `mitata` package) are for comparing two versions of
  a small function, and easy to get wrong: the JIT can optimise a benchmark away.

## CPU profiling

```bash
node --cpu-prof src/main.ts          # writes a .cpuprofile when the process exits
node --inspect src/main.ts           # then chrome://inspect → Performance / Profiler
```

Open the profile in Chrome DevTools (or speedscope) and read the **flame graph**: width is
time. Look for wide frames of your own code: JSON (de)serialisation, regular expressions,
template rendering, crypto, sorting large arrays, compiling something per request (schemas,
regular expressions, templates). The usual fixes: do it once at startup, cache the result,
do less, stream it, or move it to a worker thread. **Clinic.js** and **0x** wrap these tools
for Node.js.

## V8 in five minutes

V8 interprets code first, then compiles **hot** functions to optimised machine code based on
what it has seen. It assumes that objects of the same "shape" (same properties, added in the
same order: a **hidden class**) keep that shape. Code stays fast when:

- objects of a kind are created the same way (a class or a factory), and properties are not
  added or deleted later (`delete obj.x` changes the shape);
- a function receives the same types each time (it is **monomorphic**);
- arrays hold one kind of element.

When an assumption breaks, V8 **deoptimises** and falls back to slower code. You rarely need
this level of detail; a profile tells you when you do.

## Memory

V8 keeps JavaScript objects in a **heap** managed by a **generational garbage collector**:
short-lived objects die cheaply in the young generation; survivors move to the old
generation, which is collected less often and more expensively. `Buffer` and other off-heap
memory count in the process's RSS but not in the heap.

```ts
process.memoryUsage();   // { rss, heapTotal, heapUsed, external, arrayBuffers }
```

### Heap limit and containers

When the old generation reaches its limit, V8 collects harder and harder, latency climbs, and
finally the process dies with "JavaScript heap out of memory". Recent Node.js versions size
the default limit from the container's memory limit, but set it explicitly in production:

```bash
node --max-old-space-size=384 src/main.ts     # in MB; leave room below a 512 MB container limit
```

### Leaks

A leak in JavaScript is memory that is still **reachable**, so the GC may not free it. The
usual suspects:

| Leak | Fix |
| --- | --- |
| A cache (`Map`, object) that only grows | A bounded LRU or TTL cache (lru-cache), or `WeakMap` keyed by objects |
| Listeners added per request to a long-lived emitter | `once`, `off`, signals |
| Timers (`setInterval`) never cleared, each holding a closure | `clearInterval` when the owner closes |
| Closures keeping big objects alive | Keep only what you need |
| Global arrays used as queues without a limit | Bound them, apply backpressure |

To find one: watch `heapUsed` grow steadily under constant load, then take **heap snapshots**
some minutes apart (`v8.writeHeapSnapshot()`, DevTools, or
`--heapsnapshot-near-heap-limit=2`) and compare them in DevTools: sort by what grew and follow
the retainers to the code that holds them.

## Scaling

One Node.js process uses one core for JavaScript. To serve more:

1. **Make each request cheaper**: the profile, caching (in process with limits, or Redis),
   fewer and batched database queries, compression at the proxy, HTTP keep-alive to
   dependencies.
2. **Run more processes**: in containers, more **replicas** behind a load balancer (the usual
   way); on a single machine, the `cluster` module or a process manager forks one process per
   core sharing a port. Processes share nothing, so keep state (sessions, caches that must be
   consistent, rate limits) in Redis or a database.
3. **Move slow work out of the request**: a queue (BullMQ on Redis, RabbitMQ, SQS) and
   workers that process it, so the API answers "accepted" quickly.
4. **Move CPU work off the event loop**: worker threads, or a service in another language.

Node.js is a weaker fit for long CPU-heavy computation, very large in-memory data sets in one
process, and work that needs shared-memory parallelism everywhere. Use the right tool for
those parts and keep Node.js for what it is good at: lots of concurrent I/O.

## Summary

- Load test and read p50/p99; profile with `--cpu-prof` and flame graphs.
- Keep object shapes stable; compile and allocate once, not per request.
- Know the heap: generational GC, `--max-old-space-size` below the container limit.
- Leaks are reachable memory: unbounded caches, listeners, timers; compare heap snapshots.
- Scale with cheaper requests, more replicas (or `cluster`), queues and worker threads.

## Further reading

- Node.js, "Profiling Node.js Applications": <https://nodejs.org/en/learn/getting-started/profiling>
- Node.js, "Memory" diagnostics guide: <https://nodejs.org/en/learn/diagnostics/memory>
- Node.js, "Using Heap Snapshot": <https://nodejs.org/en/learn/diagnostics/memory/using-heap-snapshot>
- Node.js API, `--cpu-prof`: <https://nodejs.org/api/cli.html#--cpu-prof>
- Node.js API, `--max-old-space-size`: <https://nodejs.org/api/cli.html#--max-old-space-sizesize-in-mib>
- Node.js API, "Cluster": <https://nodejs.org/api/cluster.html>
- V8 blog, "Fast properties in V8": <https://v8.dev/blog/fast-properties>
- V8 blog, "Trash talk: the Orinoco garbage collector": <https://v8.dev/blog/trash-talk>
- Chrome DevTools, "Fix memory problems": <https://developer.chrome.com/docs/devtools/memory-problems>
