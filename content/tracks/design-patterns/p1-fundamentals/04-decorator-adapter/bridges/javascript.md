---
from: javascript
status: draft
concept: "Adapter, decorator, proxy and facade"
from_way: "Overwriting methods on a library object, or adding behaviour in every caller."
native_way: "Adapter modules that wrap SDKs, higher-order functions that wrap functions (`withRetry(fn)`), the built-in `Proxy` object, and facades like `fetch` over HTTP."
sensei_tip: "`new Proxy(target, { get(t, k) { ... } })` intercepts any property access: Vue's reactivity and many mocking libraries are built on it."
---

| JavaScript | Pattern |
| --- | --- |
| a module translating a REST SDK to your types | adapter |
| `withRetry(fn)`, Express middleware | decorator |
| `new Proxy(target, handler)` | proxy |
| `Object.freeze(obj)` | protection (by restriction) |
| `fetch` over sockets and HTTP | facade |
