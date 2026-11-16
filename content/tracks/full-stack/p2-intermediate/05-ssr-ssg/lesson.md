# Rendering Strategies: SSR, SSG, ISR and React Server Components

Phase 1 compared server-rendered pages with SPAs. Modern frameworks (Next.js, Nuxt, SvelteKit,
Remix and React Router, Astro, Angular SSR) let you mix them **per route**, and even per
component. The question is no longer "SPA or not" but "for this route, when is the HTML made?"

## The four basic strategies

| Strategy | HTML is made... | Fresh data? | Cost per view | First paint |
| --- | --- | --- | --- | --- |
| **CSR** (client-side rendering) | in the browser, after the JS loads | yes | a static shell + API calls | slow on weak devices |
| **SSR** (server-side rendering) | on the server, per request | yes | server work every request | fast |
| **SSG** (static site generation) | at build time | as of the last build | almost zero (CDN) | fastest |
| **ISR** (incremental static regeneration) | at build time, then regenerated after a TTL or on demand | slightly stale | almost zero | fastest |

ISR is "SSG with a TTL": the first visitor after the TTL gets the cached page while a new one is
built in the background (stale-while-revalidate for whole pages). **On-demand revalidation**
regenerates a page when the CMS publishes an edit.

## Hydration and its cost

SSR and SSG send HTML that looks finished, but buttons do nothing until the JavaScript for the
page has downloaded, parsed and **hydrated**: re-run the components in the browser to attach event
handlers and state. On a slow phone that gap can be seconds, and it hurts **INP**
(Interaction to Next Paint). The whole page's JavaScript is shipped, even for parts that never
change.

Techniques that reduce it:

- **Streaming SSR**: send the shell at once and stream slower parts as they are ready
  (`<Suspense>` boundaries), so a slow query does not block the whole page.
- **Islands** (Astro, Fresh): the page is static HTML with a few interactive islands; only the
  islands ship JavaScript.
- **React Server Components** (Next.js App Router, React Router): components marked as server
  components run only on the server, can read the database directly, and send their output, not
  their code. Only `"use client"` components ship JavaScript and hydrate.
- **Lazy hydration**: hydrate a component when it becomes visible or is first used.

## Choosing per route

Ask two questions: **where does the data come from**, and **who is looking**?

| Route | Good fit |
| --- | --- |
| marketing pages, docs, blog posts | SSG (rebuild or revalidate on publish) |
| product or article pages with many views and rare edits | ISR with on-demand revalidation |
| search results, a home feed that changes per minute | SSR with short shared caching, or ISR with a short TTL |
| per-user pages (dashboard, account) | SSR with private data, or CSR behind the login |
| highly interactive tools (editors, boards) | CSR inside a server-rendered shell |

Two traps:

- **Personal data in a shared cache.** An SSR page that includes the user's name must not be
  cached by a CDN for everyone. Mark it `private`, or render the personal bits on the client.
- **Waterfalls.** A component that fetches, then renders a child that fetches, then another,
  adds round trips. Fetch in parallel at the route level, or let server components fetch next to
  the data.

## Measuring: Core Web Vitals

Judge a strategy by real-user metrics, at the 75th percentile:

- **LCP** (Largest Contentful Paint) under 2.5 s: SSR/SSG and an optimised hero image help most.
- **INP** (Interaction to Next Paint) under 200 ms: less JavaScript and less hydration help most.
- **CLS** (Cumulative Layout Shift) under 0.1: reserve space for images, ads and late content.

The performance lesson of phase 3 shows how to collect them from real users.

## Summary

- CSR, SSR, SSG and ISR differ in when HTML is made, how fresh it is and what each view costs.
- Hydration makes server HTML interactive but ships and re-runs JavaScript; streaming, islands
  and server components cut that cost.
- Choose per route from the data's freshness and the audience; never share-cache personal pages.
- Check the choice with Core Web Vitals.

## Further reading

- web.dev, "Rendering on the Web": <https://web.dev/articles/rendering-on-the-web>
- React documentation, "Server Components": <https://react.dev/reference/rsc/server-components>
- Next.js documentation, "Incremental Static Regeneration (ISR)": <https://nextjs.org/docs/app/guides/incremental-static-regeneration>
- Astro documentation, "Islands architecture": <https://docs.astro.build/en/concepts/islands/>
- web.dev, "Web Vitals": <https://web.dev/articles/vitals>
