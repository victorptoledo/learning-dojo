---
from: go
status: reviewed
concept: "Static generation and server rendering in Go"
from_way: "Go serves JSON, and pages are a separate JavaScript project."
native_way: "Hugo builds static sites, html/template renders per request, templ adds typed components, and htmx or small islands add interaction without a full SPA."
sensei_tip: "A Go server rendering HTML with templ and a CDN in front often beats an SPA on every Core Web Vital."
---

| Go world | Rendering strategy |
| --- | --- |
| Hugo | SSG |
| `html/template` or templ per request | SSR |
| CDN + `stale-while-revalidate` | ISR-like |
| htmx or Alpine.js widgets | islands |
