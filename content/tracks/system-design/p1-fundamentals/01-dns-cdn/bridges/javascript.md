---
from: javascript
status: draft
concept: "Routing and caching at the edge"
from_way: "A bundler producing app.js and a Node server or a static host serving it."
native_way: "The bundler fingerprints assets, the CDN caches them forever, and HTML is cached briefly or revalidated; the edge can even run small functions."
sensei_tip: "Vite and webpack add content hashes to file names; give those immutable one-year headers and keep index.html short-lived, or users get a new HTML pointing to old bundles."
---

| JavaScript | Edge concept |
| --- | --- |
| `app.[contenthash].js` | fingerprinted asset, cache forever |
| `res.set('Cache-Control', 's-maxage=60')` | CDN lifetime for a page |
| Service Worker cache | a cache in the browser, before the CDN |
| `dns.promises.resolve4(host)` | a DNS lookup with its records |
| edge functions | code at the PoP: redirects, A/B, auth checks |
