---
from: javascript
status: draft
concept: "Declare the state, let a tool converge to it"
from_way: "React renders the UI from state: you describe what the DOM should be, React computes the changes."
native_way: "Configuration management does the same for servers: you describe the desired state, the tool computes and applies only the differences."
sensei_tip: "The reconcile-the-difference idea returns in Terraform plans and Kubernetes controllers; it is the core pattern of this track."
---

| JavaScript world | Configuration management |
| --- | --- |
| a component's desired output | a resource's desired state |
| virtual DOM diffing | check, then change only what differs |
| re-rendering with the same state | an idempotent second run |
| `.env` files | variables and a secrets manager |
