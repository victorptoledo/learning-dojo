# Testing Frontends: Unit, DOM, Accessibility and End-to-End

Every kata in this track was checked by the kind of tests this lesson teaches: a real browser,
user-like interactions, accessibility scans and measured layout. Now you write them yourself,
and decide which kind each behavior deserves.

## The testing trophy

| Kind | Checks | Runs in | Speed | Tools |
| --- | --- | --- | --- | --- |
| **Static** | Types, lint, formatting | The editor and CI | Instant | TypeScript, ESLint, Prettier, Stylelint, html-validate |
| **Unit** | Pure functions: formatting, validation, reducers | Node.js | Milliseconds | Vitest, node:test, Jest |
| **Component / integration** | A component or a page area with its DOM and events | jsdom/happy-dom, or a real browser | Fast | Testing Library, Vitest browser mode, Playwright component tests |
| **End-to-end** | Whole user journeys against a running app | A real browser | Seconds | Playwright, Cypress, WebdriverIO |
| **Visual regression** | Pixels compared with approved screenshots | A real browser | Seconds | Playwright `toHaveScreenshot`, Chromatic, Percy |

Put most effort in **integration** tests: they exercise real behavior and survive refactors.
Unit-test tricky logic; keep a few end-to-end tests for the critical journeys (sign-up,
checkout, login).

## Test like a user

Testing Library's principle: *the more your tests resemble the way your software is used, the
more confidence they give you*. Find elements the way users and assistive technology do:

```js
// Playwright
await page.getByRole("button", { name: "Add to cart" }).click();
await page.getByLabel("Email").fill("ada@example.com");
await expect(page.getByRole("status")).toHaveText("Saved");

// Testing Library
const button = screen.getByRole("button", { name: /add to cart/i });
await userEvent.click(button);
expect(await screen.findByText("1 item in your cart")).toBeVisible();
```

- Prefer `getByRole` with a name, then `getByLabel`, `getByText`; use test ids only when
  nothing else fits. A test that finds the button by role also proves it **is** a button with
  that accessible name.
- Avoid CSS selectors tied to markup details (`.btn-primary > span`): they break on every
  redesign without any user-visible change.
- Assert what the user sees (text, visibility, URL, focus), not internal state.

## Async and flaky tests

Flaky tests are worse than none: people learn to ignore red.

- **Wait for conditions, never for time.** Playwright's web-first assertions
  (`await expect(locator).toHaveText(...)`) and locators retry until they pass or time out;
  `findBy*` does the same in Testing Library. `sleep(500)` is a bug waiting for a slow CI
  machine.
- **Control time** with fake clocks (`page.clock`, `vi.useFakeTimers()`) for debounces,
  timeouts and animations.
- **Control the network**: mock APIs at the network level (`page.route`, Mock Service
  Worker) with realistic responses, including errors and slow answers.
- Isolate tests: a fresh browser context, storage and data per test; no order dependencies.

## jsdom or a real browser?

jsdom implements the DOM in Node.js: fast and good for logic and events, but it has **no
layout** (every box is 0×0), no real CSS cascade for many properties, no navigation, no
`IntersectionObserver` and no rendering metrics. Use a real browser (Playwright, Vitest's
browser mode) for anything about layout, focus, CSS, accessibility trees or performance.

## Accessibility in tests

```js
import AxeBuilder from "@axe-core/playwright";
const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag22aa"]).analyze();
expect(results.violations).toEqual([]);
```

Run axe on key pages and states (menu open, errors shown, dark mode), and write keyboard tests:
Tab order, Enter and Space, Escape closing dialogs, focus moving where it should. Automated
checks catch roughly a third to a half of the issues; keep manual screen-reader passes for
new components.

## What not to test

- Third-party code and the browser itself.
- Implementation details: private functions, component state, the exact number of renders.
- Snapshot dumps of large DOM trees: they get approved without being read.

## Tests in CI

Run static checks and unit tests on every push, integration and end-to-end tests on every pull
request, in the browsers you support (Playwright runs Chromium, Firefox and WebKit). Keep
traces, screenshots and videos of failures (`trace: "on-first-retry"`), and quarantine and fix
flaky tests instead of retrying them forever.

## Summary

- Static checks, unit, integration, end-to-end and visual tests each have a job; most value is
  in integration tests.
- Query by role and label, act like a user, assert what users see.
- Wait for conditions, control time and the network, isolate every test.
- Layout, focus and CSS need a real browser; jsdom has no layout.
- Automate accessibility checks and keyboard tests, and still test manually.

## Further reading

- Playwright documentation, "Best Practices": <https://playwright.dev/docs/best-practices>
- Playwright documentation, "Locators": <https://playwright.dev/docs/locators>
- Testing Library, "Guiding Principles": <https://testing-library.com/docs/guiding-principles>
- Testing Library, "Which query should I use?": <https://testing-library.com/docs/queries/about#priority>
- Vitest documentation, "Browser Mode": <https://vitest.dev/guide/browser/>
- web.dev, "Testing web applications": <https://web.dev/learn/testing>
- Deque, axe-core on GitHub: <https://github.com/dequelabs/axe-core>
