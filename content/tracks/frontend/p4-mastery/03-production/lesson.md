# Frontend in Production: Monitoring, Errors, Browser Support and Delivery

Shipping is the beginning. In production your code runs on thousands of devices you have never
seen, behind extensions, proxies and flaky networks. A senior frontend engineer owns what
happens there: knowing when it breaks, how fast it is for real users, which browsers it must
support, and how to release and roll back safely.

## Observe real users

- **Error monitoring**: catch what reaches the user.

  ```js
  window.addEventListener("error", (event) => report({ message: event.message, stack: event.error?.stack }));
  window.addEventListener("unhandledrejection", (event) => report({ reason: String(event.reason) }));
  ```

  Tools such as Sentry, Bugsnag or an OpenTelemetry browser SDK group errors, symbolicate
  stack traces with uploaded **source maps**, and attach the release, the browser and the
  steps that led there. Filter noise from extensions and third-party scripts (`crossorigin`
  plus CORS headers turn "Script error." into real messages).
- **RUM** (real user monitoring): send Core Web Vitals and custom timings with the
  `web-vitals` library and `navigator.sendBeacon`, segmented by page type, device class,
  country and release. Watch the 75th percentile, not the average.
- **Synthetic checks**: a scheduled Playwright journey (sign in, search, checkout) against
  production catches outages before users report them.
- Respect privacy: collect only what you need, avoid personal data in URLs and error
  messages, honor consent rules (GDPR, LGPD), and sample.

## Browser support

- Decide a **support policy** from your analytics: for example "Baseline Widely available"
  features plus the last two versions of the major browsers.
- **Baseline** (on MDN and web.dev) says when a feature works in all major engines: *Newly
  available* (in all of them) and *Widely available* (for 30 months).
- Use features with **feature detection** and fallbacks, not user-agent sniffing:

  ```js
  if ("scheduler" in window && "yield" in scheduler) { await scheduler.yield(); }
  else { await new Promise((resolve) => setTimeout(resolve, 0)); }
  ```

  ```css
  @supports (container-type: inline-size) { /* container query layout */ }
  ```

- Configure build targets (`browserslist`) so transpiling and polyfills match the policy, and
  run end-to-end tests in Chromium, Firefox and WebKit.

## Resilience

- The network fails: timeouts with `AbortSignal.timeout`, retries with backoff for idempotent
  requests, clear error states, and nothing typed is ever lost.
- Scripts fail: progressive enhancement means a failed bundle leaves usable HTML; an error
  boundary (in frameworks) or a top-level `try/catch` shows a recovery message instead of a
  blank page.
- **Offline**: a service worker can cache the app shell and API responses (Workbox helps), but
  it is a second deployment channel: version it, and plan how a broken worker is replaced.
- Third parties: load them `async`, isolate them in iframes when possible, and set timeouts so
  a slow tag manager does not take your page down.

## Delivering changes safely

- **Atomic, immutable deploys**: hashed assets uploaded first, then the HTML switched; keep
  old assets for a while so open tabs can still load their chunks (or handle chunk-load errors
  by reloading).
- **Feature flags**: deploy code dark, enable it for staff, then a percentage of users, and
  turn it off without a deploy if metrics drop.
- **Canary and staged rollouts** with dashboards for errors and Web Vitals per release; a
  rollback is one click.
- **Preview deployments** per pull request (Netlify, Vercel, Cloudflare Pages) let reviewers,
  designers and tests use the change before it merges.
- **CI gates**: types, lint, tests, accessibility checks, bundle budgets and Lighthouse
  thresholds must pass before a merge.

## Internationalization basics

Use `lang` and `dir` attributes, logical CSS properties for right-to-left languages, and the
`Intl` APIs (`Intl.NumberFormat`, `DateTimeFormat`, `PluralRules`, `RelativeTimeFormat`) instead
of hand-made formatting. Never build sentences by concatenating translated fragments.

## Owning it

Write short runbooks for the frontend's failure modes (a bad deploy, a broken third party, a
CDN outage), take part in incident reviews, and turn each incident into a test, an alert or a
budget. Performance, accessibility and reliability are features with owners, not side effects.

## Summary

- Monitor errors with source maps, measure Web Vitals from real users, and run synthetic
  journeys.
- Base browser support on data and Baseline; use feature detection and `@supports`.
- Design for failure: timeouts, retries, no lost input, progressive enhancement, careful
  service workers and third parties.
- Release with atomic deploys, feature flags, staged rollouts, previews and CI gates.
- Internationalize with `Intl` and logical properties; own the frontend's operations.

## Further reading

- web.dev, "Baseline": <https://web.dev/baseline>
- MDN, "Implementing feature detection": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Testing/Feature_detection>
- MDN, `Window: error event`: <https://developer.mozilla.org/en-US/docs/Web/API/Window/error_event>
- MDN, `Navigator.sendBeacon()`: <https://developer.mozilla.org/en-US/docs/Web/API/Navigator/sendBeacon>
- MDN, "Service Worker API": <https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API>
- MDN, `Intl`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl>
- web.dev, "Best practices for measuring Web Vitals in the field": <https://web.dev/articles/vitals-field-measurement-best-practices>
- W3C, "Internationalization techniques: authoring HTML & CSS": <https://www.w3.org/International/techniques/authoring-html>
