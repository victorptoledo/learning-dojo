// What a Frontend kata's tests use. A test file calls `openSite()` once at the top level: it
// serves the kata folder on 127.0.0.1, starts one headless Chromium for the file and closes both
// when the file's tests are done.
//
//   import { openSite, assertNoPageErrors, assertAccessible } from "dojo-web";
//   const site = await openSite();
//   test("the page has one h1", async () => {
//     const page = await site.open("/index.html", { width: 375 });
//     assert.equal(await page.locator("h1").count(), 1);
//     await assertAccessible(page);
//   });
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
import { after, afterEach } from "node:test";
import { chromium } from "playwright-core";
import { HtmlValidate } from "html-validate";
import { parseHeadersFile, startServer } from "./server.mjs";

export { parseHeadersFile, startServer };

const require = createRequire(import.meta.url);
let axeSource = null;
let browserPromise = null;

/** One headless Chromium per test file, started on first use. */
export function browser() {
  browserPromise ??= chromium.launch({
    headless: true,
    args: ["--disable-dev-shm-usage", "--no-first-run", "--disable-extensions"],
  });
  return browserPromise;
}

async function closeBrowser() {
  if (browserPromise) {
    const running = await browserPromise.catch(() => null);
    browserPromise = null;
    await running?.close().catch(() => {});
  }
}

const isPlaceholder = (message) => /^todo\b/i.test(message.trim());

/**
 * Serves the kata folder and returns the site:
 * - `origin` and `url(path)`;
 * - `requests`: every request the server saw ({ method, path, query, headers, body });
 * - `open(path, options)`: a new page in a fresh browser context (empty storage), loaded and
 *   ready. Options: width, height (default 1280x800), colorScheme, reducedMotion,
 *   javaScriptEnabled, storage (localStorage entries set before the page loads), clock
 *   (true installs Playwright's fake clock before the page loads and pauses it once
 *   loaded: then only page.clock.runFor() moves timers), waitUntil.
 *   A page whose scripts threw a "todo ..." error while loading fails right away with it.
 *
 * Options: root (the kata folder), routes and headers (see server.mjs).
 */
export async function openSite(options = {}) {
  const server = await startServer(options);
  const contexts = new Set();
  const closeContexts = async () => {
    for (const context of contexts) {
      await context.close().catch(() => {});
    }
    contexts.clear();
  };
  // Each test gets fresh pages: the ones it opened are closed when it ends.
  afterEach(closeContexts);
  after(async () => {
    await closeContexts();
    await closeBrowser();
    await new Promise((resolve) => server.close(resolve));
  });

  return {
    origin: server.origin,
    requests: server.requests,
    url: (pathname = "/") => new URL(pathname, server.origin).href,
    async open(pathname = "/index.html", pageOptions = {}) {
      const {
        width = 1280,
        height = 800,
        colorScheme = "light",
        reducedMotion = "no-preference",
        javaScriptEnabled = true,
        storage = null,
        clock = false,
        waitUntil = "load",
      } = pageOptions;
      const context = await (await browser()).newContext({
        viewport: { width, height },
        colorScheme,
        reducedMotion,
        javaScriptEnabled,
        serviceWorkers: "block",
      });
      contexts.add(context);
      // Short waits: a missing element fails in seconds, not at the test's time limit.
      context.setDefaultTimeout(3000);
      const page = await context.newPage();
      watch(page);
      if (storage) {
        await page.addInitScript((entries) => {
          if (!sessionStorage.getItem("__dojo_seeded")) {
            for (const [key, value] of Object.entries(entries)) {
              localStorage.setItem(key, value);
            }
            sessionStorage.setItem("__dojo_seeded", "1");
          }
        }, storage);
      }
      if (clock) {
        await page.clock.install();
      }
      const response = await page.goto(new URL(pathname, server.origin).href, { waitUntil });
      if (response && !response.ok()) {
        throw new Error(`${pathname} answered ${response.status()}: is the file there?`);
      }
      if (clock) {
        // The fake clock flows at real pace until paused: from now on only the test moves it.
        const now = await page.evaluate(() => Date.now());
        await page.clock.pauseAt(now + 1);
        page.dojoClock = true;
      }
      const todo = page.dojoErrors.find(isPlaceholder);
      if (todo) {
        throw new Error(todo);
      }
      return page;
    },
  };
}

/** Records uncaught errors and console errors on the page (page.dojoErrors, page.dojoConsole). */
function watch(page) {
  page.dojoErrors = [];
  page.dojoConsole = [];
  page.on("pageerror", (error) => page.dojoErrors.push(error.message || String(error)));
  page.on("console", (message) => {
    if (message.type() === "error") {
      page.dojoConsole.push(message.text());
    }
  });
}

/** Fails with the first uncaught error the page's scripts threw (a "todo" one first). */
export function assertNoPageErrors(page) {
  const errors = page.dojoErrors ?? [];
  const first = errors.find(isPlaceholder) ?? errors[0];
  if (first) {
    throw new Error(isPlaceholder(first) ? first : `the page threw an error: ${first}`);
  }
}

/** Waits for pending timers and microtasks a little, then checks for errors. */
export async function settle(page, ms = 50) {
  await page.waitForTimeout(ms);
  assertNoPageErrors(page);
}

/**
 * Runs axe-core on the page (or on `include`, a CSS selector) and returns the violations as
 * { id, impact, help, targets }. Tags default to WCAG 2.2 A and AA plus best practices.
 */
export async function axeViolations(page, { include = null, tags, disable = [] } = {}) {
  if (page.dojoClock) {
    // axe-core waits on timers: let the fake clock flow again.
    await page.clock.resume();
  }
  axeSource ??= readFileSync(require.resolve("axe-core/axe.min.js"), "utf8");
  // evaluate() is not subject to the page's Content-Security-Policy, so axe also runs there.
  if (!(await page.evaluate(() => typeof window.axe === "object"))) {
    await page.evaluate(axeSource);
  }
  const result = await page.evaluate(
    async ({ include, tags, disable }) => {
      const rules = Object.fromEntries(disable.map((id) => [id, { enabled: false }]));
      const outcome = await window.axe.run(include ?? document, {
        runOnly: { type: "tag", values: tags },
        rules,
        resultTypes: ["violations"],
      });
      return outcome.violations.map((violation) => ({
        id: violation.id,
        impact: violation.impact,
        help: violation.help,
        targets: violation.nodes.map((node) => node.target.join(" ")),
      }));
    },
    {
      include,
      tags: tags ?? ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa", "best-practice"],
      disable,
    },
  );
  return result;
}

/** Fails with the list of axe-core violations, if there are any. */
export async function assertAccessible(page, options = {}) {
  const violations = await axeViolations(page, options);
  if (violations.length > 0) {
    const lines = violations.map(
      (v) => `${v.id} (${v.impact}): ${v.help} → ${v.targets.slice(0, 3).join(", ")}`,
    );
    throw new Error(`axe-core found ${violations.length} accessibility problem(s):\n${lines.join("\n")}`);
  }
}

// html-validate's recommended rules, without the ones that are only about formatting style.
const HTML_RULES = {
  "void-style": "off",
  "no-trailing-whitespace": "off",
  "attribute-boolean-style": "off",
  "attribute-empty-style": "off",
  "doctype-style": "off",
  "element-case": "off",
  "attr-case": "off",
  "attr-quotes": "off",
  "require-sri": "off",
  "no-inline-style": "off",
};

/** html-validate messages for a file of the kata, as "line:column rule: message" strings. */
export async function htmlProblems(file, rules = {}) {
  const validator = new HtmlValidate({
    extends: ["html-validate:recommended"],
    rules: { ...HTML_RULES, ...rules },
  });
  const report = await validator.validateFile(path.resolve(file));
  return report.results.flatMap((result) =>
    result.messages.map(
      (message) => `${path.basename(file)}:${message.line}:${message.column} ${message.ruleId}: ${message.message}`,
    ),
  );
}

/** Fails with html-validate's messages for the file, if there are any. */
export async function assertValidHtml(file, rules = {}) {
  const problems = await htmlProblems(file, rules);
  if (problems.length > 0) {
    throw new Error(`html-validate found ${problems.length} problem(s):\n${problems.slice(0, 8).join("\n")}`);
  }
}

/** The border box of the first element matching `selector`: { x, y, width, height }. */
export async function box(page, selector) {
  const found = await page.locator(selector).first().boundingBox();
  if (!found) {
    throw new Error(`no visible element matches ${selector}`);
  }
  return found;
}

/** Computed styles of the first element matching `selector`, for the given properties. */
export function computed(page, selector, properties) {
  return page.locator(selector).first().evaluate(
    (element, names) => {
      const style = getComputedStyle(element);
      return Object.fromEntries(names.map((name) => [name, style.getPropertyValue(name)]));
    },
    properties,
  );
}
