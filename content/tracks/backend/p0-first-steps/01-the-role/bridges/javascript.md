---
from: javascript
status: draft
concept: "Beyond the framework"
from_way: "Thinking in Express routes and npm packages."
native_way: "The whole path of a request and the systems around the code."
sensei_tip: "An Express handler is the smallest part of a request's life. Timeouts, retries, caches and databases decide most of the user's experience."
---

| JavaScript / TypeScript | In practice |
| --- | --- |
| app.get(...) | one hop of the request |
| npm package for everything | a dependency with its own failure modes |
| console.log | structured logs and traces |
| works on localhost | works under load, with failures |
