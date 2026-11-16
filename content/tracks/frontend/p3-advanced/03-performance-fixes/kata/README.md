# Performance Fixes: CLS, LCP and Long Tasks

The Daily Kata's home page has three Core Web Vitals problems. Fix them in `index.html`,
`style.css` and `app.js`. The tests load the page in Chromium with **slow images** (they arrive
about 300 ms late, as on a real network) and measure with the browser's own Performance APIs:
`layout-shift` entries for CLS and `longtask` entries for main-thread blocking.

1. **Layout shifts (CLS).** The `<img>` tags have no `width` and `height`, so the browser does not
   know their size until they arrive, and the text below jumps. And `app.js` inserts a banner
   at the top of the page 400 ms after load, pushing everything down.
   - Give every image its intrinsic `width` and `height` attributes (the hero is 1200 × 600,
     the feature images 400 × 300). With `max-width: 100%; height: auto` in the CSS, the
     browser reserves the right space at any width.
   - Reserve the banner's space before it arrives: `#banner-slot` gets a `min-height` of the
     banner's height (3rem). The banner must still appear.
   - The cumulative layout shift must stay below **0.01** at 1280 and at 375 pixels.
2. **The LCP image.** The hero is the largest element on the first screen, but it is marked
   `loading="lazy"`, which delays it. Load it eagerly with `fetchpriority="high"`; the three
   feature images, below the fold, get `loading="lazy"`.
3. **A long task (INP).** "Sort by popularity" calls `rankStories`, which scores 300 stories in
   one go, about 300 ms during which the page cannot react to anything.
   - Write `yieldToMain()`: `scheduler.yield()` where the browser has it, otherwise a
     `Promise` resolved by `setTimeout(resolve, 0)`.
   - Make `rankStories(stories)` **async**: score the stories in chunks of about 10 and
     `await yieldToMain()` between chunks. Same order as before: score descending, then id.
     Do not change `scoreStory`; it is slow on purpose.
   - The click handler shows `"Sorting…"` at once, awaits the ranking, renders the top 10 and
     then shows `"Sorted by popularity."`. No task may take 50 ms or more.

To look at it yourself, open the page with the DevTools' Performance panel recording (throttle
the network to see the shifts):

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 12 tests: 1 in `tests/visible.test.js` and 11 hidden ones.
