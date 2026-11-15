---
from: javascript
status: draft
concept: "Layered, content-addressed artifacts"
from_way: "npm stores packages by integrity hash in its cache, and package-lock.json records those hashes."
native_way: "Images use the same idea for whole file systems: every layer and config is stored and fetched by its SHA-256 digest."
sensei_tip: "node_modules is often the biggest layer. Keep it in its own layer created from package.json and the lock file only, so editing your code does not rebuild it."
---

| JavaScript | Image |
| --- | --- |
| `"integrity": "sha512-..."` in the lock file | a layer or manifest digest |
| `npm dist-tag` such as `latest` or `next` | an image tag, movable the same way |
| `node_modules` | usually one large layer |
| the npm cache | the engine's layer store |
