---
from: javascript
status: draft
concept: "Networking in JavaScript"
from_way: "Ignoring that fetch hides connection setup."
native_way: "Browsers and Node's fetch (undici) pool connections; net and dgram expose TCP and UDP in Node; DNS lookups use the OS resolver on the thread pool unless you use dns.resolve."
sensei_tip: "In Node, reuse an undici Agent or the global dispatcher, set timeouts with AbortSignal.timeout, and remember dns.lookup blocks a thread-pool slot."
---

| In JavaScript / TypeScript | Underneath |
| --- | --- |
| `fetch(url)` | pooled connections (keep-alive) |
| `net.connect` | TCP socket |
| `dgram.createSocket('udp4')` | UDP |
| `dns.lookup` vs `dns.resolve4` | OS resolver vs direct DNS queries |
| `AbortSignal.timeout(5000)` | a request deadline |
