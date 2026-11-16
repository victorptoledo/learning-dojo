---
from: javascript
status: reviewed
concept: "From create-react-app to per-route strategies"
from_way: "A client-rendered SPA where every route shows a blank page and a spinner first."
native_way: "A meta-framework (Next.js, Remix and React Router, Nuxt, SvelteKit, Astro) where each route is static, revalidated, server-rendered or client-only, with server components and streaming."
sensei_tip: "Mark components \"use client\" only where you need state or effects; everything else stays on the server and ships no JavaScript."
---

| SPA habit | Per-route strategy |
| --- | --- |
| everything client-rendered | SSG or ISR for content routes |
| `useEffect` fetch on load | fetch in a server component or loader |
| one big bundle | only client components hydrate |
| waiting for slow data | `<Suspense>` streaming |
