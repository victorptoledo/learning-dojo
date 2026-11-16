---
from: javascript
status: draft
concept: "Operating a browser app"
from_way: "Operating Node services: process managers, health checks, logs and APM."
native_way: "Browser apps run on uncontrolled devices: you collect errors and metrics from them, support many engines and deploy static assets atomically."
sensei_tip: "There is no restart button for code already running in users' tabs: plan for old versions in the wild (chunk errors, stale service workers)."
---

| JavaScript / Node.js | In the browser |
| --- | --- |
| `process.on("uncaughtException")` | `window.addEventListener("error")` |
| APM agent | RUM and error-tracking SDKs |
| Node version matrix | Browser support policy (browserslist, Baseline) |
| Blue-green deploy | Atomic deploy with hashed assets |
