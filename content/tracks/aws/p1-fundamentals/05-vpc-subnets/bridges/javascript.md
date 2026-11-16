---
from: javascript
status: draft
concept: "A private network you design: routes decide who can reach whom"
from_way: "On a PaaS, the database URL is public and protected only by a password."
native_way: "The database has no public address; it lives in a private subnet reachable only from the app's security group."
sensei_tip: "`ETIMEDOUT` from a Node.js client means no path (route or security group), `ECONNREFUSED` means you reached the host and nothing listens: they point to different fixes."
---

| Node.js world | AWS |
| --- | --- |
| a public `DATABASE_URL` | a private endpoint in a data subnet |
| an IP allow list on the DB | a security group reference |
| `ETIMEDOUT` | check routes, security groups and NACLs |
| an outbound proxy | a NAT gateway |
