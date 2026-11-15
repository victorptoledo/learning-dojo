---
from: python
status: draft
concept: "Routing and caching at the edge"
from_way: "Serving static files with Django or Flask and caching views with a decorator."
native_way: "Static files and cacheable pages are served by a CDN close to the user; your app only sets the headers and handles dynamic requests."
sensei_tip: "Django's ManifestStaticFilesStorage fingerprints file names; with it you can send a max-age of a year for /static/ and never purge."
---

| Python | Edge concept |
| --- | --- |
| `@cache_control(public=True, s_maxage=60)` | shared-cache lifetime for the CDN |
| `ManifestStaticFilesStorage` / WhiteNoise | fingerprinted, immutable assets |
| `socket.getaddrinfo(host, 443)` | a DNS lookup through the OS resolver |
| `@cache_page(60)` | an app-side cache, behind the CDN |
| `dnspython` | inspecting records and TTLs from code |
