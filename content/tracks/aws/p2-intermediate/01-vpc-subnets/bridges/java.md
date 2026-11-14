---
from: java
status: draft
concept: "Secure network topology"
from_way: "Giving the database machine a public IP so you can connect from home."
native_way: "Databases and application servers live in private subnets with no public IP; only load balancers live in public subnets."
sensei_tip: "Reach servers in private subnets with AWS Systems Manager Session Manager instead of opening SSH port 22 to the internet."
---
