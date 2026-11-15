---
from: python
status: draft
concept: "Beyond the framework"
from_way: "Thinking in Django or FastAPI terms: views, models and the ORM."
native_way: "Thinking in requests, data, failures and operations, whatever the framework."
sensei_tip: "The framework hides DNS, TLS, the load balancer, connection pools and timeouts, but production problems live there. Learn to follow one request end to end."
---

| Python | In practice |
| --- | --- |
| a Django view | one step in a request's journey |
| manage.py runserver | a service behind a load balancer |
| the ORM | a connection pool and SQL |
| print debugging | logs, metrics and traces |
