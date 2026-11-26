# Checkpoint 3: a Launch-Ready Product Page

The Dojo Tee goes on sale tomorrow, and its product page has every problem of this phase: it
jumps while it loads, the main photo arrives late, a script blocks the parser, the reviews
trust strangers' HTML, every visitor downloads the size guide, there is no
Content-Security-Policy, and after each deploy some visitors get an old `index.html` for an
hour. Fix `index.html`, `app.js` and `_headers`. `style.css`, `analytics.js`, `size-guide.js`
and the images are done.

The tests serve the page with a fake `GET /api/reviews` (four reviews, some of them attacks, or
a `503`), and the images arrive 300 ms late, as on a real network.

## `index.html`: rendering and Core Web Vitals

- `analytics.js` must not block the parser: nothing waits for it, so give it `defer` (or
  `async`).
- The inline `<script>` goes: move its line into `app.js` (a strict CSP refuses inline code).
- The hero image is the page's **Largest Contentful Paint**: no `loading="lazy"`,
  `fetchpriority="high"`, and `width="1200" height="900"`.
- The three thumbnails: `width="300" height="300"` and `loading="lazy"`.
- With the dimensions in place, the page's **CLS stays below 0.01** while the images arrive.

## `app.js`: safe rendering and a lazy chunk

| Code | What it does |
| --- | --- |
| `safeUrl(value)` | `url.href` of `new URL(value.trim())` when the protocol is `http:` or `https:`, otherwise `null` (also for relative, malformed, blank or non-string values) |
| `reviewElement(review)` | `<li class="review">` with `<p class="review-author">`, `<p class="review-rating">` (`"Rated N out of 5"`) and `<p class="review-text">`, all built with `createElement` and `textContent`. When `safeUrl(website)` gives a URL, the author's name is a link to it with `rel="nofollow ugc noopener noreferrer"` |
| `loadReviews()` | `GET /api/reviews`; shows the reviews in `#reviews` in one DOM write and `"N reviews"` (`"1 review"`) in `#reviews-status`. When the request fails or is not `ok`: `"Reviews are unavailable right now."` |
| `openSizeGuide()` | Toggles `#size-guide` (`hidden`) and the button's `aria-expanded`. The first time it opens, it loads `./size-guide.js` with a **dynamic `import()`** and calls `renderSizeGuide(guide)`; later opens reuse the table |

On load, `app.js` adds the class `js` to `<html>`, listens for clicks on the size guide button
and loads the reviews. The static `import` at the top of the starter has to go: the tests check
that `size-guide.js` is only requested when the guide opens.

## `_headers`: security and caching

For `/*`:

- keep `X-Content-Type-Options: nosniff` and add a `Referrer-Policy`;
- `Content-Security-Policy` with `default-src 'self'`, `script-src 'self'` (nothing else),
  `object-src 'none'`, `base-uri 'none'` (or `'self'`), `form-action 'self'`,
  `frame-ancestors 'none'` and `require-trusted-types-for 'script'`;
- `Cache-Control: no-cache`, so a visitor's browser always checks for a new `index.html` (and
  the other files without a hash in their names).

For `/assets/*`, whose file names carry a content hash: `Cache-Control: public,
max-age=31536000, immutable`. A later rule overrides the header an earlier rule set.

The page must still work under its own policy, with no CSP or Trusted Types violations.

To try it in your own browser (the serve command applies `_headers` too, but has no reviews
API):

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 16 tests: 1 in `tests/visible.test.js` and 15 hidden ones.
