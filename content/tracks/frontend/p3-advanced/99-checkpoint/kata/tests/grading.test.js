import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import {
  openSite,
  assertAccessible,
  assertNoPageErrors,
  assertValidHtml,
  parseHeadersFile,
  settle,
  startServer,
} from "dojo-web";

const REVIEWS = [
  { author: "Ada", website: "https://example.com/ada", rating: 5, text: "Survived a hundred katas." },
  { author: "Mallory", website: "javascript:window.pwned='link'", rating: 1, text: '<img src=x onerror="window.pwned=\'img\'">' },
  { author: '<script>window.pwned="author"</script>', website: "", rating: 4, text: "Soft <b>and</b> warm" },
  { author: "Eve", website: "data:text/html,<script>alert(1)</script>", rating: 3, text: "<svg onload=\"window.pwned='svg'\"></svg>" },
];
const IMAGES = ["tee-hero.5d41a2.svg", "tee-front.8f14e4.svg", "tee-back.c9f0f8.svg", "tee-detail.45c48c.svg"];

let reviewsFail = false;
// The images arrive late, as they do on a real network, so missing dimensions cause shifts.
const slowImage = (file) => () => ({
  headers: { "content-type": "image/svg+xml" },
  body: readFileSync(`assets/${file}`),
  delayMs: 300,
});
const site = await openSite({
  routes: {
    // The 503 body still parses as a list, so only response.ok tells the failure apart.
    "GET /api/reviews": () => (reviewsFail ? { status: 503, json: [] } : { json: REVIEWS, delayMs: 100 }),
    ...Object.fromEntries(IMAGES.map((file) => [`GET /assets/${file}`, slowImage(file)])),
  },
});

/** Cumulative Layout Shift of the page so far (shifts right after user input do not count). */
function cumulativeLayoutShift(page) {
  return page.evaluate(
    () =>
      new Promise((resolve) => {
        let total = 0;
        new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            if (!entry.hadRecentInput) total += entry.value;
          }
        }).observe({ type: "layout-shift", buffered: true });
        setTimeout(() => resolve(total), 100);
      }),
  );
}

/** The headers rule of _headers for a pattern ({} when missing). */
function headersFor(pattern) {
  const rules = parseHeadersFile(readFileSync("_headers", "utf8"));
  return rules.find((rule) => rule.pattern === pattern)?.headers ?? {};
}

/** The Content-Security-Policy of /*, as { directive: [values] }. */
function policy() {
  const csp = headersFor("/*")["content-security-policy"];
  assert.ok(csp, "the /* rule of _headers sets Content-Security-Policy");
  return Object.fromEntries(
    csp
      .split(";")
      .map((part) => part.trim().split(/\s+/))
      .filter((words) => words[0])
      .map(([name, ...values]) => [name.toLowerCase(), values]),
  );
}

async function reviewsLoaded(page) {
  await page.locator("#reviews li").nth(REVIEWS.length - 1).waitFor();
  await settle(page, 100);
}

const guideButton = (page) => page.getByRole("button", { name: "Size guide" });
const sizeGuideRequests = () => site.requests.filter((request) => request.path === "/size-guide.js").length;

test("the HTML is valid and has no inline scripts or event handlers", async () => {
  await assertValidHtml("index.html");
  const html = readFileSync("index.html", "utf8");
  assert.doesNotMatch(html, /<script(?![^>]*\bsrc=)[^>]*>/i, "move inline <script> code into app.js");
  assert.doesNotMatch(html, /\son[a-z]+\s*=/i, "replace on...= attributes with addEventListener");
});

test("no script in <head> blocks the parser", async () => {
  const page = await site.open("/index.html");
  const scripts = await page.$$eval("head script", (all) =>
    all.map((s) => ({ src: s.getAttribute("src"), module: s.type === "module", defer: s.defer, async: s.async })),
  );
  assert.ok(scripts.some((s) => s.src === "analytics.js"), "keep loading analytics.js");
  for (const script of scripts) {
    assert.ok(script.module || script.defer || script.async, `${script.src} blocks the parser: add defer (or async)`);
  }
  assert.equal(await page.evaluate(() => window.analytics?.events?.[0]?.name), "page_view", "analytics.js still runs");
});

test("the hero image (the LCP element) loads eagerly with high priority and keeps its 4:3 shape", async () => {
  const page = await site.open("/index.html");
  const hero = page.locator("img.hero");
  assert.notEqual(await hero.getAttribute("loading"), "lazy", "never lazy-load the LCP image");
  assert.equal(await hero.getAttribute("fetchpriority"), "high");
  const width = Number(await hero.getAttribute("width"));
  const height = Number(await hero.getAttribute("height"));
  assert.ok(width > 0 && height > 0, "the hero needs width and height attributes");
  assert.equal(width / height, 4 / 3, "the hero is 1200 x 900: keep its aspect ratio");
});

test("the thumbnails have dimensions and load lazily", async () => {
  const page = await site.open("/index.html");
  const thumbs = await page.$$eval(".thumbs img", (all) =>
    all.map((img) => ({ width: Number(img.getAttribute("width")), height: Number(img.getAttribute("height")), loading: img.getAttribute("loading") })),
  );
  assert.equal(thumbs.length, 3);
  for (const thumb of thumbs) {
    assert.ok(thumb.width > 0 && thumb.width === thumb.height, "the thumbnails are 300 x 300");
    assert.equal(thumb.loading, "lazy");
  }
});

test("the page does not jump while it loads (CLS below 0.01)", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 800 });
  await page.waitForTimeout(1000); // images at ~300 ms, reviews at ~100 ms
  const cls = await cumulativeLayoutShift(page);
  assert.ok(cls < 0.01, `CLS is ${cls.toFixed(3)}: reserve the space of the images`);
});

test("safeUrl() keeps absolute http(s) URLs only", async () => {
  const page = await site.open("/index.html");
  const out = await page.evaluate(async () => {
    const { safeUrl } = await import("/app.js");
    return [
      " https://example.com/a b ",
      "HTTP://Example.com",
      "javascript:alert(1)",
      "  JavaScript:alert(1)",
      "data:text/html,hi",
      "/relative",
      "example.com",
      "",
      null,
      42,
    ].map(safeUrl);
  });
  assert.deepEqual(out, ["https://example.com/a%20b", "http://example.com/", null, null, null, null, null, null, null, null]);
});

test("reviewElement() builds text, not HTML, and links only safe websites", async () => {
  const page = await site.open("/index.html");
  const out = await page.evaluate(async () => {
    const { reviewElement } = await import("/app.js");
    const read = (li) => ({
      tag: li.tagName,
      className: li.className,
      author: li.querySelector(".review-author")?.textContent,
      rating: li.querySelector(".review-rating")?.textContent,
      text: li.querySelector(".review-text")?.textContent,
      elements: li.querySelectorAll("i, b, img").length,
      href: li.querySelector(".review-author a")?.getAttribute("href") ?? null,
      rel: li.querySelector(".review-author a")?.getAttribute("rel") ?? null,
    });
    return [
      read(reviewElement({ author: "<i>Bob</i>", website: "https://example.com", rating: 4, text: "<b>bold</b>" })),
      read(reviewElement({ author: "Mallory", website: "javascript:alert(1)", rating: 1, text: "<img src=x>" })),
    ];
  });
  assert.deepEqual(
    { ...out[0], rel: undefined },
    { tag: "LI", className: "review", author: "<i>Bob</i>", rating: "Rated 4 out of 5", text: "<b>bold</b>", elements: 0, href: "https://example.com/", rel: undefined },
  );
  assert.deepEqual((out[0].rel ?? "").split(/\s+/).sort(), ["nofollow", "noopener", "noreferrer", "ugc"]);
  assert.equal(out[1].href, null, "a javascript: website is not a link");
  assert.equal(out[1].author, "Mallory", "the name stays, as text");
  assert.equal(out[1].elements, 0);
});

test("loaded reviews show every payload as plain text and run none of it", async () => {
  reviewsFail = false;
  const page = await site.open("/index.html");
  await reviewsLoaded(page);
  assert.equal(await page.evaluate(() => window.pwned), undefined, "a review ran code");
  assert.equal(await page.locator("#reviews img, #reviews script, #reviews svg, #reviews b").count(), 0);
  assert.equal(await page.locator("#reviews .review-text").nth(1).innerText(), REVIEWS[1].text);
  const hrefs = await page.$$eval("#reviews a", (links) => links.map((a) => a.getAttribute("href")));
  assert.deepEqual(hrefs, ["https://example.com/ada"]);
  assert.equal(await page.locator("#reviews-status").innerText(), "4 reviews");
});

test("when the reviews API fails, the page says so and keeps working", async () => {
  reviewsFail = true;
  try {
    const page = await site.open("/index.html");
    await page.getByText("Reviews are unavailable right now.").waitFor();
    assert.equal(await page.locator("#reviews li").count(), 0);
    await guideButton(page).click();
    await page.getByRole("table").waitFor();
    assertNoPageErrors(page);
  } finally {
    reviewsFail = false;
  }
});

test("the size guide is a lazy chunk: downloaded only when it is opened", async () => {
  const before = sizeGuideRequests();
  const page = await site.open("/index.html");
  await reviewsLoaded(page);
  assert.equal(sizeGuideRequests(), before, "size-guide.js was downloaded on load: use import() when the guide opens");
  await guideButton(page).click();
  await page.getByRole("table", { name: "Size guide (cm)" }).waitFor();
  assert.equal(sizeGuideRequests(), before + 1);
  assert.equal(await page.locator("#size-guide tbody tr").count(), 4);
});

test("the size guide button toggles the guide and says whether it is open", async () => {
  const page = await site.open("/index.html");
  const button = guideButton(page);
  assert.equal(await button.getAttribute("aria-expanded"), "false");
  await button.click();
  await page.getByRole("table").waitFor();
  assert.equal(await button.getAttribute("aria-expanded"), "true");
  const firstTable = await page.locator("#size-guide table").elementHandle();
  await button.click();
  await settle(page);
  assert.equal(await page.locator("#size-guide").isHidden(), true);
  assert.equal(await button.getAttribute("aria-expanded"), "false");
  await button.click();
  await settle(page);
  assert.equal(await page.locator("#size-guide").isVisible(), true);
  assert.equal(await page.locator("#size-guide table").count(), 1, "render the guide once");
  assert.equal(await firstTable.evaluate((table) => table.isConnected), true, "reopening reuses the table rendered the first time");
});

test("the CSP allows scripts from the site only and closes the other doors", () => {
  const csp = policy();
  assert.deepEqual(csp["default-src"], ["'self'"], "default-src 'self'");
  const scripts = csp["script-src"] ?? csp["default-src"];
  assert.deepEqual(scripts, ["'self'"], `script-src is ${scripts.join(" ")}: use 'self' and nothing else`);
  assert.deepEqual(csp["object-src"], ["'none'"]);
  assert.ok(["'none'", "'self'"].includes(csp["base-uri"]?.[0]), "base-uri 'none' or 'self'");
  assert.deepEqual(csp["frame-ancestors"], ["'none'"], "frame-ancestors 'none' stops clickjacking");
  assert.deepEqual(csp["form-action"], ["'self'"]);
  assert.deepEqual(csp["require-trusted-types-for"], ["'script'"], "turn on Trusted Types");
  assert.equal(headersFor("/*")["x-content-type-options"], "nosniff");
  assert.ok(headersFor("/*")["referrer-policy"], "set Referrer-Policy (for example strict-origin-when-cross-origin)");
});

test("the page works under its own policy: Trusted Types on, no CSP violations", async () => {
  const page = await site.open("/index.html");
  await reviewsLoaded(page);
  await guideButton(page).click();
  await page.getByRole("table").waitFor();
  const sink = await page.evaluate(() => {
    try {
      document.querySelector("#reviews").innerHTML = "<li>x</li>";
      return "allowed";
    } catch (error) {
      return error.name;
    }
  });
  assert.equal(sink, "TypeError", "innerHTML with a string must be refused");
  assert.equal(await page.evaluate(() => document.documentElement.classList.contains("js")), true, "app.js adds the js class");
  const violations = page.dojoConsole.filter((text) => /Content.Security.Policy|Trusted Type/i.test(text));
  assert.deepEqual(violations, []);
  assertNoPageErrors(page);
});

test("hashed assets are cached for a year, and index.html is revalidated on every visit", async () => {
  // A server without the test routes, so every answer comes from the files and _headers.
  const server = await startServer({ root: "." });
  try {
    const cacheControl = async (pathname) =>
      ((await fetch(new URL(pathname, server.origin))).headers.get("cache-control") ?? "").toLowerCase();
    for (const file of IMAGES) {
      const value = await cacheControl(`/assets/${file}`);
      assert.match(value, /immutable/, `/assets/${file}: ${value || "no Cache-Control"}; add immutable`);
      const maxAge = Number(/max-age=(\d+)/.exec(value)?.[1] ?? 0);
      assert.ok(maxAge >= 31536000, `/assets/${file}: max-age is ${maxAge}; cache hashed files for a year`);
    }
    for (const pathname of ["/index.html", "/app.js"]) {
      const value = await cacheControl(pathname);
      assert.ok(
        /no-cache|max-age=0\b/.test(value) && !/immutable/.test(value),
        `${pathname}: Cache-Control is "${value}"; files without a hash need no-cache`,
      );
    }
  } finally {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("axe-core finds no problems, with the reviews and the guide shown", async () => {
  const page = await site.open("/index.html");
  await reviewsLoaded(page);
  await guideButton(page).click();
  await page.getByRole("table").waitFor();
  await assertAccessible(page);
});
