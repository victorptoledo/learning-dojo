---
from: javascript
status: draft
concept: "Recovery is designed, tested and timed"
from_way: "A static site is on a CDN, so it never seems to go down."
native_way: "The back ends behind it need explicit RTO and RPO, replicated data, and a failover that has been timed in a game day."
sensei_tip: "Serve a static degraded page from the CDN when the origin fails: a cheap way to shorten what users experience as downtime."
---

| JavaScript world | Disaster recovery |
| --- | --- |
| a CDN with many edges | multi-region serving |
| a service worker cache | graceful degradation |
| a status banner | communication during failover |
| browser retries | client behaviour during failover |
