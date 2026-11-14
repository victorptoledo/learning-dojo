---
from: java
status: draft
concept: "From RPC endpoints to resources"
from_way: "Endpoints named after methods (/getUserById, /saveOrder), entities serialised as they are, a list of every row, and a new endpoint for every change."
native_way: "Resources and HTTP methods, input and output records, paginated collections, merge-patch updates protected by ETags, Problem Details errors and an OpenAPI contract."
sensei_tip: "Write the OpenAPI (or at least the request and response examples) before the controller: designing the contract first avoids leaking the database into the API."
---

| RPC style | Resource style |
| --- | --- |
| `POST /getOrders` | `GET /orders?status=open&page=0` |
| `POST /updateOrderStatus` | `PATCH /orders/42` `{"status": "shipped"}` |
| `POST /deleteUser?id=7` | `DELETE /users/7` |
| `{"success": false, "msg": "..."}` with 200 | `409` + Problem Details |
