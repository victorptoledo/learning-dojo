# The Full Stack Role and the Journey of a Request

A **full stack developer** can take a feature from an idea to production on their own: the page
the user sees, the API it calls, the data it stores and the way it is deployed and watched. That
does not mean being the deepest expert in every layer. It means understanding every layer well
enough to make the pieces fit, to notice when a decision in one layer hurts another, and to know
when to ask a specialist.

## The pillars

| Pillar | What you do | Where to go deeper in the dojo |
| --- | --- | --- |
| The web platform | HTML, forms, the DOM, CSS, accessibility, `fetch` | the Frontend and React tracks |
| HTTP and APIs | methods, status codes, headers, cookies, CORS, contracts | Backend Developer |
| A server language | routing, validation, business rules, tests | TypeScript, Modern Java, Spring Boot |
| Data | modelling, SQL, migrations, indexes, transactions | SQL & Relational Databases |
| Security | sessions and tokens, CSRF, XSS, injection | Backend Developer, Cyber Security |
| Delivery | containers, environments, CI/CD | Docker & Containers, Git & GitHub |
| Operations | logs, metrics, traces, incidents | Backend Developer, System Design |

This track is the **slice through all of them**. It does not re-teach SQL or TypeScript from
scratch; it shows how they meet. Its katas use TypeScript on Node.js, Spring Boot, PostgreSQL and
Docker Compose, the tools you would meet in a typical product team.

## The journey of one click

A user clicks **Save** on a form. Here is what happens, and what you own at each step.

1. **The browser** runs your JavaScript (or submits the form natively). It builds an HTTP
   request: method, URL, headers (cookies, `Content-Type`), body.
2. **DNS** turns `shop.example.com` into an IP address. A wrong record or an expired domain fails
   here, before your code runs.
3. **TCP and TLS**: the browser opens a connection and checks the server's certificate. An
   expired certificate is a full outage that no log of yours will show.
4. **A CDN or reverse proxy** (Cloudflare, CloudFront, nginx) may answer from its cache, compress,
   terminate TLS and forward the request.
5. **A load balancer** picks one instance of your server.
6. **Your server** routes the request, authenticates the user (session cookie or token),
   validates the input, applies the business rules.
7. **The database** runs the queries inside a transaction; constraints refuse bad data even if
   your code forgot to.
8. **The response** goes back with a status, headers (`Set-Cookie`, `Cache-Control`, `ETag`) and
   a body, usually JSON or HTML.
9. **The browser** updates the page: replaces a list, shows an error under a field, or redirects.

> [!TIP]
> Each arrow in that list is a place where the request can fail, be slow, or be cached. Most
> "it works on my machine" bugs live in steps 2–5, which do not exist on your machine.

## Who sees what when it fails

| Symptom | Where to look first |
| --- | --- |
| Button does nothing | browser console (a JavaScript error), the network tab (no request sent) |
| `CORS error` in the console | the API's CORS headers: the request may even have succeeded on the server |
| `401` / `403` | the cookie or token sent, its expiry, the authorization rule |
| `400` / `422` | the request body versus the API's contract |
| `500` | the server log for that request, ideally found by its trace id |
| Slow page | the network tab's timing, then the server's latency metric, then the query plan |

Learning to move between these views quickly, from the browser to the server log to the
database and back, is the core skill of the role.

## Monolith first

Most full stack work happens in a single deployable application with one database, sometimes a
separate SPA. That is a good default: one codebase, one deploy, transactions that just work. You
will meet services, queues and edge functions in later phases, as answers to specific problems,
not as a starting point.

## Summary

- The role owns a feature end to end: page, API, data, deployment and operations.
- One click crosses the browser, DNS, TLS, a CDN, a load balancer, your server and the database.
- Every hop can fail; knowing where each failure shows up is half of debugging.
- Start with a monolith and add moving parts only when a problem demands them.

## Further reading

- roadmap.sh, "Full Stack Developer": <https://roadmap.sh/full-stack>
- MDN, "How the web works": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Getting_started/Web_standards/How_the_web_works>
- MDN, "An overview of HTTP": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Overview>
- web.dev, "How browsers work": <https://web.dev/articles/howbrowserswork>
