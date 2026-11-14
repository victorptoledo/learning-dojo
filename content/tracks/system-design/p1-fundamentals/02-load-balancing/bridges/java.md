---
from: java
status: draft
concept: "Transport layer vs application layer"
from_way: "A basic reverse proxy doing round-robin in front of a few instances."
native_way: "L4 balancers (TCP/UDP) route raw connections at huge volume; L7 balancers (HTTP) read headers, cookies and paths to route between services."
sensei_tip: "With consistent hashing, only about 1/N of the keys move when a node leaves, which keeps caches and sticky routing stable."
---
