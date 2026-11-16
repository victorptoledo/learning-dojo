# Core Web Vitals: LCP, INP and CLS

"Fast" means different things: the page shows its content quickly, reacts when you tap, and
does not jump around while you read. The **Core Web Vitals** measure exactly those three
experiences on real users' devices, and they feed Google Search ranking, so product teams
watch them closely.

## The three metrics

| Metric | Measures | Good | Poor |
| --- | --- | --- | --- |
| **LCP**, Largest Contentful Paint | When the largest image or text block in the viewport is painted | ≤ 2.5 s | > 4 s |
| **INP**, Interaction to Next Paint | The latency of the slowest (98th percentile) interaction of the visit: from a tap, click or key press to the next frame | ≤ 200 ms | > 500 ms |
| **CLS**, Cumulative Layout Shift | How much visible content moves unexpectedly (the largest burst of shifts) | ≤ 0.1 | > 0.25 |

They are judged at the **75th percentile** of page loads, split by phone and desktop: three
quarters of your users must have a good experience. INP replaced FID (First Input Delay) in
March 2024, because FID measured only the first interaction and only its input delay.

Supporting metrics help diagnose: **TTFB** (time to first byte: server and network) and **FCP**
(first contentful paint).

## LCP: show the main content early

LCP is made of four parts: TTFB, the delay before the LCP resource starts loading, its load
time, and the delay until it is rendered. Fix the biggest part:

- **Server**: cache HTML at the edge (a CDN), avoid slow redirects and long server work.
- **Discoverability**: the LCP image must be in the HTML as an `<img>` (not a CSS background
  or something JavaScript inserts later), so the preload scanner finds it.
- **Priority**: `fetchpriority="high"` on the LCP image and **never** `loading="lazy"` on it.
- **Size**: modern formats, the right dimensions per screen with `srcset`.
- **Render blocking**: little CSS in `<head>`, scripts deferred, fonts that do not hide text.
- Client-rendered apps that build the page with JavaScript usually have a slow LCP: render
  the HTML on the server (SSR or static generation) when LCP matters.

## INP: answer quickly

An interaction has three phases: **input delay** (the main thread is busy with something
else), **processing** (your event handlers), and **presentation delay** (style, layout and
paint of the next frame).

- Break up **long tasks** (over 50 ms): yield with `scheduler.yield()` or
  `setTimeout`, process in chunks, move heavy work to a Web Worker.
- Do the **visible feedback first** (set "Saving…", toggle the pressed state), then the rest
  after yielding.
- Avoid huge DOM updates and layout thrashing in handlers; debounce work triggered by typing.
- Watch third-party scripts (tag managers, chat widgets): they compete for the same thread.

## CLS: keep things where they are

A layout shift happens when a visible element moves without a recent user input. Common
causes and fixes:

| Cause | Fix |
| --- | --- |
| Images and videos without dimensions | `width` and `height` attributes (or `aspect-ratio` in CSS) |
| Ads, embeds, banners injected above content | Reserve their space with `min-height`, or put them below or over the content |
| Content inserted by JavaScript after the first paint | Render it in the HTML, reserve its space or insert it below the viewport |
| Web fonts that swap with different metrics | `font-display: optional`, preload the font, `size-adjust` on the fallback |
| Animating `top`/`height` | Animate `transform` instead |

Shifts within 500 ms of a user's input do not count: a menu opening because you clicked is
expected.

## Measuring: lab and field

- **Lab data** (Lighthouse, DevTools' Performance panel, WebPageTest) is reproducible and good
  for debugging, but it is one device and one network.
- **Field data** (RUM, real user monitoring) is what users experience: the Chrome UX Report
  (CrUX), PageSpeed Insights, Search Console, or your own collection with the `web-vitals`
  library:

```js
import { onLCP, onINP, onCLS } from "web-vitals";
const send = (metric) => navigator.sendBeacon("/rum", JSON.stringify(metric));
onLCP(send); onINP(send); onCLS(send);
```

- The browser APIs underneath: `PerformanceObserver` with entry types `largest-contentful-paint`,
  `event`, `layout-shift` and `longtask`.

Always confirm a lab improvement in the field: the 75th percentile of real devices is often a
mid-range phone on a mobile network.

## Performance budgets

Agree on limits and check them in CI: for example LCP under 2.5 s on a throttled Lighthouse
run, at most 170 KB of compressed JavaScript for the first screen, no image without
dimensions. Budgets turn performance into a rule instead of a hope.

## Summary

- LCP (≤ 2.5 s) is loading, INP (≤ 200 ms) is responsiveness, CLS (≤ 0.1) is visual stability,
  all at the 75th percentile.
- LCP: a discoverable, high-priority, well-sized hero, fast HTML and little render blocking.
- INP: short tasks, feedback first, yield often, workers for heavy work.
- CLS: dimensions on media, reserved space for late content, careful fonts.
- Debug in the lab, judge in the field.

## Further reading

- web.dev, "Web Vitals": <https://web.dev/articles/vitals>
- web.dev, "Largest Contentful Paint (LCP)": <https://web.dev/articles/lcp>
- web.dev, "Interaction to Next Paint (INP)": <https://web.dev/articles/inp>
- web.dev, "Cumulative Layout Shift (CLS)": <https://web.dev/articles/cls>
- web.dev, "Optimize Cumulative Layout Shift": <https://web.dev/articles/optimize-cls>
- W3C, Largest Contentful Paint specification: <https://w3c.github.io/largest-contentful-paint/>
- MDN, `PerformanceObserver`: <https://developer.mozilla.org/en-US/docs/Web/API/PerformanceObserver>
- GitHub, GoogleChrome/web-vitals: <https://github.com/GoogleChrome/web-vitals>
