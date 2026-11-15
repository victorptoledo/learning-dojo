---
from: python
status: draft
concept: "From RPC endpoints to resources"
from_way: "Function-shaped endpoints such as /get_orders or /create_order, and FastAPI's automatic OpenAPI."
native_way: "Resource-oriented URLs, the right methods, pagination, filtering, partial updates and Problem Details errors."
sensei_tip: "FastAPI generates the OpenAPI document for free; design the resources first and let the document describe a good API rather than a list of functions."
---

| Python | In practice |
| --- | --- |
| POST /create_order | POST /orders |
| GET /get_order?id=1 | GET /orders/1 |
| page and page_size params | limit and cursor or page params with limits |
| {'error': '...'} | application/problem+json |
