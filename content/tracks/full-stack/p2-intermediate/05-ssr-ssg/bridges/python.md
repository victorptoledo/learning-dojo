---
from: python
status: reviewed
concept: "From Django views to SSG and ISR"
from_way: "Every page rendered by Django on every request, even pages that change once a month."
native_way: "Pre-render rarely changing pages (a static generator or a CDN with long TTLs), keep per-request rendering for personal pages, and let interactive islands hydrate on the client."
sensei_tip: "A CDN in front of a Django view with s-maxage and stale-while-revalidate is a cheap form of ISR, as long as the page is not personal."
---

| Python world | Rendering strategy |
| --- | --- |
| Django views per request | SSR |
| Pelican, MkDocs, `distill` | SSG |
| CDN + `stale-while-revalidate` | ISR-like |
| htmx fragments | small islands of interactivity |
