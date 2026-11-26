# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 85%, so 7 right.
2. **The integration kata** (press **Enter**): a product page made ready for launch. It must
   load without blocking scripts or layout shifts, render strangers' reviews as text, download
   the size guide only when someone opens it, and ship with a strict Content-Security-Policy and
   cache headers that survive a deploy.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| The rendering pipeline | Which resources block parsing or rendering, and what do `defer`, `async` and `type="module"` change? Does this change cost layout, paint or only compositing? Why do interleaved reads and writes thrash? |
| Core Web Vitals | What are the good thresholds of LCP, INP and CLS, and at which percentile? What delays the LCP image? Which kind of change shifts the layout, and how do you reserve space? Why can the lab be green while the field is red? |
| Browser security | Which DOM sinks parse HTML or run code? Why validate a URL's protocol? What does each CSP directive close, and what do Trusted Types add? What does CORS protect, and why can `*` not go with credentials? |
| Build tools | What does a bundler add to native modules? When does `import()` create a chunk? Which files get immutable caching and which get `no-cache`? Why is every build-time variable public? |

> [!TIP]
> The kata touches all four lessons at once. Fix one file at a time and rerun the checks: the
> test names say which part of the page they look at.
