---
from: java
status: draft
concept: "Networks, load balancers and bills are part of the design"
from_way: "The data centre team gives you a VIP on the F5 and a firewall rule after a ticket."
native_way: "You declare the subnets, security groups and load balancer in Terraform, and you see what your service costs each month."
sensei_tip: "Connection pools in the JVM hold connections open; size them per instance times instances, or a scale-out exhausts the database's max_connections."
---

| Java world | Cloud |
| --- | --- |
| an F5 VIP | a layer 7 load balancer |
| a firewall ticket | a security group rule in code |
| a DMZ | public subnets |
| HikariCP pool size | connections per instance × instances |
