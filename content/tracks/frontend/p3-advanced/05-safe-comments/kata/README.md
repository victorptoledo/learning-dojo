# Safe Comments: Stop XSS and Lock It Down with CSP

The comments widget in `app.js` builds HTML out of strangers' text with `innerHTML`. A comment
like `<img src=x onerror="...">` runs code in every visitor's browser: **cross-site scripting**.
Fix it twice: render safely, then add a **Content-Security-Policy** so that a mistake you make
later is blocked by the browser too (defense in depth). The tests load comments full of attacks
from a fake `GET /api/comments` and check that none of them runs.

## `app.js`

| Code | What it does |
| --- | --- |
| `safeUrl(value)` | Parses with `new URL(value.trim())`; returns `url.href` when the protocol is `http:` or `https:`, otherwise `null` (also for relative, malformed, blank or missing values). This stops `javascript:` and `data:` links |
| `commentElement(comment)` | `<li class="comment">` with `<p class="author">` and `<p class="body">`, built with `createElement` and `textContent`. When `safeUrl(website)` gives a URL, the author name is a link to it with `rel="nofollow ugc noopener noreferrer"` |
| `loadComments()` | Fetches `/api/comments` and shows them |

Also move the inline `<script>` of `index.html` into `app.js`, and replace the button's
`onclick="..."` with `addEventListener`: a strict CSP refuses both.

## `_headers`

The dojo's server (and hosts like Netlify or Cloudflare Pages) reads response headers from
`_headers`. For `/*`, keep `X-Content-Type-Options: nosniff` and add:

- `Referrer-Policy`, for example `strict-origin-when-cross-origin`;
- `Content-Security-Policy` with at least:
  - `default-src 'self'` and `script-src 'self'` (no `'unsafe-inline'`, no `'unsafe-eval'`, no
    hosts or wildcards);
  - `object-src 'none'`, `base-uri 'none'` (or `'self'`), `form-action 'self'`,
    `frame-ancestors 'none'`;
  - `require-trusted-types-for 'script'`: with **Trusted Types**, assigning a plain string to
    `innerHTML` throws a `TypeError`, so an XSS sink cannot creep back in.

The page must still work under the policy, with no CSP violations in the console.

To try it in your own browser (the serve command applies `_headers` too, but has no comments
API):

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 13 tests: 1 in `tests/visible.test.js` and 12 hidden ones.
