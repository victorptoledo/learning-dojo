---
from: python
status: draft
concept: "Spreading load and owning keys"
from_way: "A dict hashes keys into buckets; Gunicorn workers behind NGINX share requests round robin."
native_way: "A balancer spreads requests by health and load; a cache or data tier places keys with consistent hashing so a server change moves only about 1/N of them."
sensei_tip: "Python randomizes str hashes per process (PYTHONHASHSEED), so hash(key) differs between your servers. For placement, use a stable hash such as hashlib.md5 or mmh3, never the built-in hash()."
---

| Python | Load balancing concept |
| --- | --- |
| `hash(key) % n` | naive placement; also not stable across processes |
| `bisect.bisect(ring, h)` | the next node clockwise on a sorted ring |
| `hashlib.md5(key.encode()).digest()` | a stable hash for placement |
| Gunicorn `--graceful-timeout` | draining in-flight requests |
| `pymemcache.HashClient` | client-side placement over many servers |
