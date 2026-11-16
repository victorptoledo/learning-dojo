import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { openSite, assertAccessible, assertNoPageErrors, parseHeadersFile, settle } from "dojo-web";

const COMMENTS = [
  { author: "Ada", website: "https://example.com/ada", body: "Lovely kata." },
  { author: "Mallory", website: "javascript:window.pwned='link'", body: '<img src=x onerror="window.pwned=\'img\'">' },
  { author: '<script>window.pwned="author"</script>', website: "", body: "Hi <b>there</b>" },
  { author: "Eve", website: "data:text/html,<script>alert(1)</script>", body: "<svg onload=\"window.pwned='svg'\"></svg>" },
];

const site = await openSite({ routes: { "GET /api/comments": () => ({ json: COMMENTS }) } });

/** The Content-Security-Policy that _headers gives to every page, as { directive: [values] }. */
function policy() {
  const rules = parseHeadersFile(readFileSync("_headers", "utf8"));
  const all = rules.find((rule) => rule.pattern === "/*");
  assert.ok(all, "_headers has a /* rule");
  const csp = all.headers["content-security-policy"];
  assert.ok(csp, "the /* rule sets Content-Security-Policy");
  return Object.fromEntries(
    csp
      .split(";")
      .map((part) => part.trim().split(/\s+/))
      .filter((words) => words[0])
      .map(([name, ...values]) => [name.toLowerCase(), values]),
  );
}

async function loaded(page) {
  await page.getByRole("button", { name: "Load comments" }).click();
  await page.locator("#comments li").nth(COMMENTS.length - 1).waitFor();
  await settle(page, 200);
}

test("safeUrl() keeps http(s) URLs only", async () => {
  const page = await site.open("/index.html");
  const out = await page.evaluate(async () => {
    const { safeUrl } = await import("/app.js");
    return [
      "https://example.com/a b",
      "http://example.com",
      " https://example.com/x ",
      "javascript:alert(1)",
      "JaVaScRiPt:alert(1)",
      "data:text/html,hi",
      "/relative/path",
      "example.com",
      "",
      undefined,
    ].map(safeUrl);
  });
  assert.deepEqual(out, [
    "https://example.com/a%20b",
    "http://example.com/",
    "https://example.com/x",
    null,
    null,
    null,
    null,
    null,
    null,
    null,
  ]);
});

test("commentElement() builds text, not HTML", async () => {
  const page = await site.open("/index.html");
  const item = await page.evaluate(async () => {
    const { commentElement } = await import("/app.js");
    const li = commentElement({ author: "<i>Bob</i>", website: "https://example.com", body: "<b>bold</b>" });
    return {
      tag: li.tagName,
      className: li.className,
      author: li.querySelector(".author")?.textContent,
      body: li.querySelector(".body")?.textContent,
      elements: li.querySelectorAll("i, b").length,
      href: li.querySelector(".author a")?.getAttribute("href"),
      rel: li.querySelector(".author a")?.getAttribute("rel"),
    };
  });
  assert.equal(item.tag, "LI");
  assert.equal(item.className, "comment");
  assert.equal(item.author, "<i>Bob</i>");
  assert.equal(item.body, "<b>bold</b>");
  assert.equal(item.elements, 0, "the text must not become elements");
  assert.equal(item.href, "https://example.com/");
  assert.deepEqual((item.rel ?? "").split(/\s+/).sort(), ["nofollow", "noopener", "noreferrer", "ugc"]);
});

test("loaded comments show every payload as plain text and run none of it", async () => {
  const page = await site.open("/index.html");
  await loaded(page);
  assert.equal(await page.evaluate(() => window.pwned), undefined, "a comment ran code");
  assert.equal(await page.locator("#comments img, #comments script, #comments svg, #comments b").count(), 0);
  assert.equal(await page.locator("#comments .body").nth(1).innerText(), COMMENTS[1].body);
  assert.equal(await page.locator("#comments .author").nth(2).innerText(), COMMENTS[2].author);
});

test("only http(s) websites become links", async () => {
  const page = await site.open("/index.html");
  await loaded(page);
  const hrefs = await page.$$eval("#comments a", (links) => links.map((a) => a.getAttribute("href")));
  assert.deepEqual(hrefs, ["https://example.com/ada"]);
  assert.equal(await page.locator("#comments .author").nth(1).innerText(), "Mallory", "the name stays, without a link");
});

test("a posted comment is shown safely too", async () => {
  const page = await site.open("/index.html");
  await page.getByLabel("Name").fill("Trudy");
  await page.getByLabel("Comment", { exact: true }).fill('<img src=x onerror="window.pwned=\'form\'">');
  await page.getByRole("button", { name: "Post" }).click();
  await settle(page, 200);
  assert.equal(await page.evaluate(() => window.pwned), undefined);
  assert.equal(await page.locator("#comments .body").innerText(), '<img src=x onerror="window.pwned=\'form\'">');
  assertNoPageErrors(page);
});

test("the HTML has no inline scripts or inline event handlers", async () => {
  const html = readFileSync("index.html", "utf8");
  assert.doesNotMatch(html, /<script(?![^>]*\bsrc=)[^>]*>/i, "move inline <script> code into app.js");
  assert.doesNotMatch(html, /\son[a-z]+\s*=/i, "replace on...= attributes with addEventListener");
});

test("the CSP allows scripts from the site only", () => {
  const csp = policy();
  const scripts = csp["script-src"] ?? csp["default-src"];
  assert.ok(scripts, "script-src (or default-src) is set");
  assert.deepEqual(scripts, ["'self'"], `script-src is ${scripts.join(" ")}: use 'self' and nothing else`);
  assert.deepEqual(csp["default-src"], ["'self'"], "default-src 'self'");
});

test("the CSP closes plugins, base tags, framing and form targets", () => {
  const csp = policy();
  assert.deepEqual(csp["object-src"], ["'none'"]);
  assert.ok(["'none'", "'self'"].includes(csp["base-uri"]?.[0]), "base-uri 'none' or 'self'");
  assert.deepEqual(csp["frame-ancestors"], ["'none'"], "frame-ancestors 'none' stops clickjacking");
  assert.deepEqual(csp["form-action"], ["'self'"]);
});

test("the CSP turns on Trusted Types, so string-to-HTML sinks fail", async () => {
  const csp = policy();
  assert.deepEqual(csp["require-trusted-types-for"], ["'script'"]);
  const page = await site.open("/index.html");
  const outcome = await page.evaluate(() => {
    try {
      document.querySelector("#comments").innerHTML = "<li>x</li>";
      return "allowed";
    } catch (error) {
      return error.name;
    }
  });
  assert.equal(outcome, "TypeError", "innerHTML with a string must be refused");
});

test("the page sends nosniff and a referrer policy", () => {
  const rules = parseHeadersFile(readFileSync("_headers", "utf8"));
  const headers = rules.find((rule) => rule.pattern === "/*")?.headers ?? {};
  assert.equal(headers["x-content-type-options"], "nosniff");
  assert.ok(headers["referrer-policy"], "set Referrer-Policy (for example strict-origin-when-cross-origin)");
});

test("the page still works under its own policy, with no CSP violations", async () => {
  const page = await site.open("/index.html");
  await loaded(page);
  assert.equal(await page.locator("#comments li").count(), COMMENTS.length);
  const violations = page.dojoConsole.filter((text) => /Content.Security.Policy|Trusted Type/i.test(text));
  assert.deepEqual(violations, []);
  assertNoPageErrors(page);
});

test("axe-core finds no problems", async () => {
  const page = await site.open("/index.html");
  await loaded(page);
  await assertAccessible(page);
});
