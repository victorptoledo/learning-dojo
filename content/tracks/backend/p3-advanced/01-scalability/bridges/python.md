---
from: python
status: draft
concept: "From one server to many"
from_way: "More gunicorn workers on a bigger machine, and state in process memory."
native_way: "Stateless instances behind a load balancer, caches, read replicas and finally partitioning."
sensei_tip: "In-process caches and sessions break when you add instances; move them to Redis or the database before scaling out."
---

| Python | In practice |
| --- | --- |
| more workers | more instances |
| session in memory | session in Redis or a token |
| DATABASE_ROUTERS for replicas | read replicas with lag in mind |
| one big database | sharding as the last step |
