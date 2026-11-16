import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite, assertAccessible, assertNoPageErrors, settle } from "dojo-web";

const BOOKS = [
  { id: 1, title: "Dune", author: "Frank Herbert" },
  { id: 2, title: "Dune Messiah", author: "Frank Herbert" },
  { id: 3, title: "Foundation", author: "Isaac Asimov" },
  { id: 4, title: "The Dispossessed", author: "Ursula K. Le Guin" },
  { id: 5, title: '<img src=x onerror="window.hacked=1">', author: "Mallory" },
];

// How slow the fake API is for some queries, in real milliseconds.
const SLOW = { du: 800, slow: 600 };

const site = await openSite({
  routes: {
    "GET /api/search": ({ query }) => {
      const q = (query.q ?? "").toLowerCase();
      if (q === "boom") {
        return { status: 500, json: { error: "the index is down" } };
      }
      const found = q === "xss" ? [BOOKS[4]] : BOOKS.slice(0, 4).filter((book) => book.title.toLowerCase().includes(q));
      return { json: found, delayMs: SLOW[q] ?? 0 };
    },
  },
});

const searches = () => site.requests.filter((request) => request.path === "/api/search");
const searchesFor = (q) => searches().filter((request) => request.query.q === q);

/** A page with Playwright's fake clock: timers only move when the test says so. */
async function open(path = "/index.html") {
  site.requests.length = 0;
  return site.open(path, { clock: true });
}

async function type(page, text) {
  await page.getByLabel("Search books").pressSequentially(text);
}

test("debounce() calls once, with the last arguments, after the pause", async () => {
  const page = await open();
  const calls = await page.evaluate(async () => {
    const { debounce } = await import("/app.js");
    const seen = [];
    const later = debounce((...args) => seen.push(args), 300);
    later("a");
    later("b", 2);
    window.__seen = seen;
    return seen.length;
  });
  assert.equal(calls, 0, "nothing runs straight away");
  await page.clock.runFor(299);
  assert.equal(await page.evaluate(() => window.__seen.length), 0, "nothing before 300 ms");
  await page.clock.runFor(1);
  assert.deepEqual(await page.evaluate(() => window.__seen), [["b", 2]]);
});

test("searchUrl() and resultLabel()", async () => {
  const page = await open();
  const out = await page.evaluate(async () => {
    const { searchUrl, resultLabel } = await import("/app.js");
    return [searchUrl("dune"), searchUrl("c & c++ 2"), resultLabel(0), resultLabel(1), resultLabel(4)];
  });
  assert.deepEqual(out, ["/api/search?q=dune", "/api/search?q=c+%26+c%2B%2B+2", "No results", "1 result", "4 results"]);
});

test("search() returns the JSON and throws on an HTTP error", async () => {
  const page = await open();
  const out = await page.evaluate(async () => {
    const { search } = await import("/app.js");
    const found = await search("found");
    const failure = await search("boom").then(() => "no error", (error) => String(error.message));
    return { found, failure };
  });
  assert.deepEqual(out.found, [BOOKS[2]]);
  assert.match(out.failure, /500/, "the error says which status came back");
});

test("typing waits for a 300 ms pause, then sends one request", async () => {
  const page = await open();
  await type(page, "dune");
  await page.clock.runFor(100);
  await type(page, " ");
  await page.clock.runFor(299);
  assert.equal(searches().length, 0, "no request while the user is still typing");
  await page.clock.runFor(1);
  await page.locator("#results li").first().waitFor();
  assert.equal(searches().length, 1);
  assert.equal(searchesFor("dune").length, 1, "the query is trimmed");
});

test("results are listed as 'Title — Author' and counted", async () => {
  const page = await open();
  await type(page, "dune");
  await page.clock.runFor(300);
  await page.locator("#results li").nth(1).waitFor();
  assert.deepEqual(await page.locator("#results li").allInnerTexts(), ["Dune — Frank Herbert", "Dune Messiah — Frank Herbert"]);
  assert.equal(await page.locator("#status").innerText(), "2 results");
  assert.equal(await page.locator("#results").getAttribute("aria-busy"), "false");
  assertNoPageErrors(page);
});

test("no match says 'No results'", async () => {
  const page = await open();
  await type(page, "zzz");
  await page.clock.runFor(300);
  await page.getByText("No results").waitFor();
  assert.equal(await page.locator("#results li").count(), 0);
});

test("while a search runs, the list is busy and the status says so", async () => {
  const page = await open();
  await type(page, "slow");
  await page.clock.runFor(300);
  await page.getByText("Searching…").waitFor();
  assert.equal(await page.locator("#results").getAttribute("aria-busy"), "true");
  await page.getByText("No results").waitFor({ timeout: 3000 });
  assert.equal(await page.locator("#results").getAttribute("aria-busy"), "false");
});

test("an HTTP error shows 'Search failed. Try again.'", async () => {
  const page = await open();
  await type(page, "dune");
  await page.clock.runFor(300);
  await page.locator("#results li").first().waitFor();
  await page.getByLabel("Search books").fill("boom");
  await page.clock.runFor(300);
  await page.getByText("Search failed. Try again.").waitFor();
  assert.equal(await page.locator("#results li").count(), 0, "old results are cleared");
  assert.equal(await page.locator("#results").getAttribute("aria-busy"), "false");
  assertNoPageErrors(page);
});

test("clearing the field clears the results without a request", async () => {
  const page = await open();
  await type(page, "dune");
  await page.clock.runFor(300);
  await page.locator("#results li").first().waitFor();
  const before = searches().length;
  await page.getByLabel("Search books").fill("   ");
  await page.clock.runFor(300);
  await settle(page);
  assert.equal(searches().length, before, "no request for a blank query");
  assert.equal(await page.locator("#results li").count(), 0);
  assert.equal(await page.locator("#status").innerText(), "");
});

test("a newer search aborts the older one, whose late answer is ignored", async () => {
  const page = await open();
  const aborted = [];
  page.on("requestfailed", (request) => aborted.push(new URL(request.url()).searchParams.get("q")));
  await type(page, "du");
  await page.clock.runFor(300);
  await type(page, "ne");
  await page.clock.runFor(300);
  await page.locator("#results li").nth(1).waitFor();
  await page.waitForTimeout(1000); // the slow answer for "du" would have arrived by now
  assert.deepEqual(aborted, ["du"], "the request for 'du' is aborted with an AbortController");
  assert.deepEqual(await page.locator("#results li").allInnerTexts(), ["Dune — Frank Herbert", "Dune Messiah — Frank Herbert"]);
  assert.equal(await page.locator("#status").innerText(), "2 results");
  assertNoPageErrors(page);
});

test("a query asked before is answered from memory", async () => {
  const page = await open();
  for (const text of ["dune", "found", "dune"]) {
    await page.getByLabel("Search books").fill(text);
    await page.clock.runFor(300);
    await page.locator("#results li").first().waitFor();
  }
  assert.equal(searchesFor("dune").length, 1, "the second 'dune' uses the cache");
  assert.equal(await page.locator("#status").innerText(), "2 results");
});

test("the query is kept in the URL and in localStorage, and restored on reload", async () => {
  const page = await open();
  await type(page, "dune");
  await page.clock.runFor(300);
  await page.locator("#results li").first().waitFor();
  assert.equal(await page.evaluate(() => new URLSearchParams(location.search).get("q")), "dune");
  assert.equal(await page.evaluate(() => localStorage.getItem("last-query")), "dune");
  await page.goto(site.url("/index.html"));
  await page.locator("#results li").first().waitFor();
  assert.equal(await page.getByLabel("Search books").inputValue(), "dune", "the field is filled in again");
});

test("?q= in the URL is searched on load, without waiting", async () => {
  const page = await open("/index.html?q=found");
  await page.locator("#results li").first().waitFor();
  assert.equal(await page.getByLabel("Search books").inputValue(), "found");
  assert.deepEqual(await page.locator("#results li").allInnerTexts(), ["Foundation — Isaac Asimov"]);
});

test("titles are shown as text, never as HTML", async () => {
  const page = await open();
  await type(page, "xss");
  await page.clock.runFor(300);
  await page.locator("#results li").first().waitFor();
  await page.waitForTimeout(200);
  assert.equal(await page.evaluate(() => window.hacked), undefined);
  assert.equal(await page.locator("#results img").count(), 0);
});

test("axe-core finds no problems with results on screen", async () => {
  const page = await open();
  await type(page, "dune");
  await page.clock.runFor(300);
  await page.locator("#results li").first().waitFor();
  await assertAccessible(page);
});
