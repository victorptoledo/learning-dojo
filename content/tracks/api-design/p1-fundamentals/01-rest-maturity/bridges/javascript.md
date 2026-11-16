---
from: javascript
status: draft
concept: "Resources and REST"
from_way: "Express routes named after functions: `/api/getOrders`, `/api/updateOrder`."
native_way: "Routers per resource: `router.get('/orders/:id')`, `router.post('/orders')`, `router.post('/orders/:id/cancellation')`."
sensei_tip: "Keep route params opaque strings; `req.params.id` is always a string, which is what an id in a URL is."
---

| JavaScript / TypeScript | REST concept |
| --- | --- |
| `router.route('/orders')` | collection |
| `router.get('/orders/:id')` | item |
| `router.post('/orders/:id/cancellation')` | action as a resource |
| `router.patch` with merge patch | partial update |
| `res.location(url)` | link to the created resource |
