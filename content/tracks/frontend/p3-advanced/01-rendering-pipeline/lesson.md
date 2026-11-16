# The Rendering Pipeline: Parse, Style, Layout, Paint, Composite

To make a page fast you must know what the browser does with your HTML, CSS and JavaScript,
and which of your changes make it redo expensive work. This lesson follows a page from bytes to
pixels and then from one frame to the next.

## The critical rendering path

1. **Parse HTML → DOM.** The parser works incrementally as bytes arrive. A preload scanner
   looks ahead for `<link>`, `<script>` and `<img>` URLs and starts fetching them early.
2. **Parse CSS → CSSOM.** CSS is **render blocking**: the browser does not paint until the
   stylesheets in `<head>` are loaded, so the page does not flash unstyled.
3. **Scripts.** A classic `<script src>` without attributes is **parser blocking**: parsing
   stops while it downloads and runs (it may call `document.write`). `defer` and
   `type="module"` download in parallel and run after parsing, in order; `async` runs as soon
   as it arrives, in any order.
4. **Style.** Match selectors to elements and compute every property's value.
5. **Layout** (reflow). Compute each box's position and size.
6. **Paint.** Record drawing operations for text, colors, borders, shadows and images, in
   layers.
7. **Composite.** The compositor thread assembles layers, often on the GPU, and puts the frame
   on screen.

```text
HTML → DOM ─┐
            ├→ Style → Layout → Paint → Composite → 🖥
CSS → CSSOM ┘
```

## What a change costs

| You change | The browser redoes |
| --- | --- |
| `width`, `height`, `top`, `margin`, `font-size`, content, adding nodes | Style → **Layout** → Paint → Composite |
| `color`, `background`, `box-shadow`, `visibility` | Style → **Paint** → Composite |
| `transform`, `opacity` (on a composited layer), `filter` in many cases | Style → **Composite** only |

That is why smooth animations move things with `transform: translate()` and fade with
`opacity`, not with `left` or `height`. `will-change: transform` hints that an element will
animate so the browser can give it a layer in advance; use it sparingly (layers cost memory).

## Frames and the main thread

At 60 Hz a frame lasts 16.7 ms. Your scripts, style, layout and paint share the **main
thread**; if a task takes 200 ms, the page drops 12 frames and ignores input. The loop per
frame looks like this:

```text
task (event handler, timer) → microtasks → requestAnimationFrame callbacks → style → layout → paint
```

- Put visual updates in `requestAnimationFrame` so they land right before the next paint.
- Split long work into chunks and **yield** (`await scheduler.yield()` or a `setTimeout(0)`
  promise) so the browser can render and handle input in between.
- Move pure computation (parsing, image processing) to a **Web Worker**: a separate thread
  that talks to the page with `postMessage`.

## Layout thrashing

Reading a layout property (`offsetHeight`, `getBoundingClientRect()`, `scrollTop`,
`getComputedStyle`) after a write forces the browser to run layout **now**, synchronously.
Alternating writes and reads in a loop forces a layout per iteration:

```js
// ❌ forced synchronous layout on every iteration
for (const card of cards) {
  card.style.height = `${card.offsetHeight + 10}px`;
}
// ✅ read everything, then write everything
const heights = cards.map((card) => card.offsetHeight);
cards.forEach((card, i) => { card.style.height = `${heights[i] + 10}px`; });
```

DevTools' Performance panel marks these as "Forced reflow" with a purple Layout event.

## Doing less work

- **Smaller DOMs** lay out faster; virtualize very long lists (render only what is visible).
- **`content-visibility: auto`** skips rendering of off-screen sections until they come near
  the viewport (with `contain-intrinsic-size` to reserve their height).
- **`contain: layout paint`** tells the browser that a widget's inside does not affect the
  outside, so changes stay local.
- Simple selectors and fewer style recalculations: change one class on a parent rather than
  styles on hundreds of children.

## Loading resources well

- Keep CSS small and in `<head>`; inline the critical part for the first screen if needed.
- Load scripts with `defer` or `type="module"`; never block the parser.
- Hint the browser: `<link rel="preload">` for a late-discovered critical file (a font, the
  hero image), `<link rel="preconnect">` for a critical third-party origin,
  `fetchpriority="high"` on the most important image.
- Images: modern formats (AVIF, WebP), `srcset`/`sizes`, `width`/`height` attributes,
  `loading="lazy"` below the fold only.
- Fonts: `font-display: swap` (or `optional`), preload the one or two you need, subset them.

## Summary

- DOM and CSSOM feed style, layout, paint and composite; CSS blocks rendering and classic
  scripts block parsing.
- Geometry changes cost a layout; colors cost a paint; transform and opacity can skip both.
- The main thread does everything: keep tasks short, yield, and use workers for heavy work.
- Never interleave layout reads and writes in a loop.
- Load critical resources early and everything else late.

## Further reading

- MDN, "Critical rendering path": <https://developer.mozilla.org/en-US/docs/Web/Performance/Guides/Critical_rendering_path>
- MDN, "How browsers work": <https://developer.mozilla.org/en-US/docs/Web/Performance/Guides/How_browsers_work>
- web.dev, "Rendering performance": <https://web.dev/articles/rendering-performance>
- web.dev, "Avoid large, complex layouts and layout thrashing": <https://web.dev/articles/avoid-large-complex-layouts-and-layout-thrashing>
- web.dev, "Optimize long tasks": <https://web.dev/articles/optimize-long-tasks>
- WHATWG HTML, "Event loops": <https://html.spec.whatwg.org/multipage/webappapis.html#event-loops>
- MDN, "Using Web Workers": <https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API/Using_web_workers>
