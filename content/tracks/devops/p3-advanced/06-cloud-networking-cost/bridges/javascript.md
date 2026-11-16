---
from: javascript
status: draft
concept: "Networks, load balancers and bills are part of the design"
from_way: "A CDN and a hosting platform hide the network, and the bill is a flat plan."
native_way: "In your own cloud account you design the VPC and load balancer yourself and pay per instance-hour, per gigabyte and per request."
sensei_tip: "Cache-Control headers are a cost control: every response a CDN serves is egress you do not pay at origin rates."
---

| JavaScript world | Cloud |
| --- | --- |
| a CDN | edge caching that cuts egress |
| a hosting plan | per-use pricing |
| `fetch` to an API | DNS, load balancer, security groups |
| `Cache-Control` | fewer origin requests |
