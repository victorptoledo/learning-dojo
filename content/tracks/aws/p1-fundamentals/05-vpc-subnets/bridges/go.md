---
from: go
status: draft
concept: "A private network you design: routes decide who can reach whom"
from_way: "A Go service listens on `:8080` on a host behind a hardware firewall."
native_way: "The service runs in private subnets; only the load balancer's security group may reach port 8080."
sensei_tip: "Set `net.Dialer{Timeout: 3 * time.Second}` on your HTTP and DB clients: a missing route shows up as a hang, and short timeouts make it obvious."
---

| Go world | AWS |
| --- | --- |
| `net.Listen("tcp", ":8080")` on a public host | a task in a private subnet behind an ALB |
| iptables | security groups and network ACLs |
| `/etc/hosts` for internal names | VPC DNS and Route 53 private zones |
| a dial timeout | the symptom of a missing route |
