# How the Web Works: From a URL to Pixels

A frontend developer builds what runs in the **browser**: the documents, styles and scripts a
user sees and touches. Before writing any of it, know what happens between typing a URL and
seeing a page, because every later topic (performance, security, caching, accessibility) hangs
on that path.

## The three languages of the platform

| Language | Job | Standard |
| --- | --- | --- |
| **HTML** | Structure and meaning: headings, paragraphs, links, forms, images | WHATWG HTML Living Standard |
| **CSS** | Presentation: layout, color, typography, responsiveness | W3C CSS specifications (many modules) |
| **JavaScript** | Behavior: reacting to events, fetching data, changing the page | ECMAScript (TC39) plus the Web APIs (WHATWG, W3C) |

They are layered on purpose. HTML must make sense on its own (a screen reader, a search engine
or a browser with JavaScript off still gets the content); CSS styles it; JavaScript enhances
it. This idea, **progressive enhancement**, comes back in every phase.

## From URL to page

1. **Parse the URL.** `https://shop.example.com:443/jackets?size=m#reviews` is a scheme,
   a host, a port (443 is implied for https), a path, a query and a fragment. The fragment
   never reaches the server.
2. **DNS** turns `shop.example.com` into an IP address (cached by the browser and the OS).
3. **Connect**: TCP plus TLS for https (or QUIC for HTTP/3). TLS proves the server owns the
   name and encrypts everything after.
4. **HTTP request**: `GET /jackets?size=m` with headers (`Accept`, `Accept-Language`,
   `Cookie`, `User-Agent`...).
5. **HTTP response**: a status (`200 OK`, `301`, `404`, `500`), headers
   (`Content-Type: text/html`, `Cache-Control`, `Content-Security-Policy`...) and the body.
6. **Parse and render.** The browser parses HTML into the **DOM** as bytes arrive. Each
   `<link rel="stylesheet">`, `<script src>` and `<img>` is a new request. CSS becomes the
   **CSSOM**; style, layout and paint turn both into pixels (Phase 3 goes deep here).
7. **Run scripts**, which can change the DOM, listen for events and make more requests.

```text
URL → DNS → TCP/TLS → HTTP GET → 200 text/html → parse HTML → DOM ─┐
                                    CSS → CSSOM ──────────────────┤→ layout → paint → screen
                                    JS  → runs, may change the DOM ┘
```

## HTTP in five lines

```http
GET /index.html HTTP/1.1
Host: example.com
Accept: text/html

HTTP/1.1 200 OK
Content-Type: text/html; charset=utf-8
Cache-Control: max-age=60
```

- **Methods**: `GET` reads (safe, cacheable), `POST` creates or acts, `PUT`/`PATCH`/`DELETE`
  change resources.
- **Status classes**: 2xx success, 3xx redirect, 4xx client error, 5xx server error.
- **Headers** carry everything else: content type, caching, cookies, security policies.
- HTTP/2 and HTTP/3 send many requests over one connection; the meaning is the same.

## An origin is a security boundary

An **origin** is scheme + host + port: `https://shop.example.com` and
`https://api.example.com` are different origins. The browser's **same-origin policy** keeps a
page from reading another origin's data. CORS (Phase 3) is the controlled way to relax it.

## Your tools

- **A browser with DevTools** (Chrome, Firefox, Edge, Safari). Learn these panels first:
  **Elements** (the live DOM and the computed styles), **Console** (errors, quick JavaScript),
  **Network** (every request, its status, headers, size and timing), and **Lighthouse** or
  **Performance** for audits.
- **An editor** with HTML, CSS and JavaScript support (VS Code works well).
- **A local server.** Opening a file with `file://` breaks ES modules and `fetch`. The dojo's
  katas include one: `docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve`.
- **References**: MDN Web Docs for every element, property and API; the specs (WHATWG, W3C)
  when MDN is not enough; "Baseline" badges on MDN and web.dev tell you which features work in
  all major browsers.

## How the dojo checks frontend katas

Your files load in a real headless **Chromium** in a container with no network. Tests written
with Node's test runner drive the page with Playwright (click, type, resize), measure boxes,
run **axe-core** for accessibility and **html-validate** for HTML. A failed test says what the
browser saw, for example "the card is 512px wide".

## Summary

- HTML for meaning, CSS for presentation, JavaScript for behavior; the content must work first.
- A page load is DNS, a connection, an HTTP request and response, then parsing and rendering
  with more requests for each resource.
- Status codes, headers and the origin are the vocabulary of the rest of the track.
- DevTools' Elements, Console and Network panels are your daily instruments.

## Further reading

- MDN, "How the web works": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Getting_started/Web_standards/How_the_web_works>
- MDN, "An overview of HTTP": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Overview>
- MDN, "Same-origin policy": <https://developer.mozilla.org/en-US/docs/Web/Security/Same-origin_policy>
- WHATWG, URL Standard: <https://url.spec.whatwg.org/>
- web.dev, "Baseline": <https://web.dev/baseline>
- Chrome DevTools documentation: <https://developer.chrome.com/docs/devtools>
