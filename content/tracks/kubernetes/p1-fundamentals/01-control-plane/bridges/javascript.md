---
from: javascript
status: draft
concept: "The declarative reconciliation loop"
from_way: "In a front-end framework you declare the UI for a state and the framework reconciles the DOM; on servers you still write imperative restart scripts."
native_way: "Kubernetes applies the same idea to infrastructure: you declare the state and controllers reconcile the cluster towards it."
sensei_tip: "React's render-then-diff is a good mental model: spec is your component's desired output, controllers are the reconciler, status is what actually rendered."
---

| Front-end idea | Kubernetes |
| --- | --- |
| state → declared UI | `spec` |
| the virtual DOM diff | a controller comparing desired and actual |
| the rendered DOM | Pods, endpoints, load balancers |
| an event emitter | an API watch |
