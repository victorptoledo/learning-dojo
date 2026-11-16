import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { openSite, assertAccessible, assertNoPageErrors, assertValidHtml, settle } from "dojo-web";

// The images arrive late, as they do on a real network, so missing dimensions cause shifts.
const slowImage = (file) => () => ({
  headers: { "content-type": "image/svg+xml" },
  body: readFileSync(file),
  delayMs: 300,
});
const site = await openSite({
  routes: Object.fromEntries(
    ["hero.svg", "story-1.svg", "story-2.svg", "story-3.svg"].map((file) => [`GET /${file}`, slowImage(file)]),
  ),
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

function expectedTop10() {
  const stories = Array.from({ length: 300 }, (_, i) => ({
    id: i + 1,
    title: `Story number ${i + 1}`,
    score: ((i * 7919) % 1000) + 10 * ((i * 104729) % 50),
  }));
  stories.sort((a, b) => b.score - a.score || a.id - b.id);
  return stories.slice(0, 10).map((story) => story.title);
}

test("the HTML is valid", async () => {
  await assertValidHtml("index.html");
});

test("every image has width and height, so its space is reserved", async () => {
  const page = await site.open("/index.html");
  const images = await page.$$eval("img", (all) =>
    all.map((img) => ({ src: img.getAttribute("src"), width: img.getAttribute("width"), height: img.getAttribute("height") })),
  );
  assert.equal(images.length, 4);
  for (const image of images) {
    assert.ok(Number(image.width) > 0 && Number(image.height) > 0, `${image.src} needs width and height attributes`);
  }
  const hero = images.find((image) => image.src === "hero.svg");
  assert.equal(Number(hero.width) / Number(hero.height), 2, "the hero is 1200 x 600: keep its aspect ratio");
});

test("the hero image (the LCP element) loads eagerly with high priority", async () => {
  const page = await site.open("/index.html");
  const hero = page.locator("img.hero");
  assert.notEqual(await hero.getAttribute("loading"), "lazy", "never lazy-load the LCP image");
  assert.equal(await hero.getAttribute("fetchpriority"), "high");
});

test("the images below the fold load lazily", async () => {
  const page = await site.open("/index.html");
  const loading = await page.$$eval(".feature img", (all) => all.map((img) => img.getAttribute("loading")));
  assert.deepEqual(loading, ["lazy", "lazy", "lazy"]);
});

test("the page does not jump while it loads (CLS below 0.01)", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 800 });
  await page.waitForTimeout(1200); // images at ~300 ms, the banner at 400 ms
  assert.equal(await page.locator(".banner").count(), 1, "the banner still appears");
  const cls = await cumulativeLayoutShift(page);
  assert.ok(cls < 0.01, `CLS is ${cls.toFixed(3)}: reserve space for the images and the banner`);
});

test("on a phone the page does not jump either", async () => {
  const page = await site.open("/index.html", { width: 375, height: 800 });
  await page.waitForTimeout(1200);
  const cls = await cumulativeLayoutShift(page);
  assert.ok(cls < 0.01, `CLS is ${cls.toFixed(3)} at 375px`);
});

test("yieldToMain() resolves on a later task", async () => {
  const page = await site.open("/index.html");
  const order = await page.evaluate(async () => {
    const { yieldToMain } = await import("/app.js");
    const seen = [];
    setTimeout(() => seen.push("timeout"), 0);
    const waiting = yieldToMain().then(() => seen.push("yielded"));
    seen.push("sync");
    await waiting;
    await new Promise((resolve) => setTimeout(resolve, 10));
    return seen;
  });
  assert.equal(order[0], "sync", "yieldToMain() must not block");
  assert.ok(order.includes("yielded"));
});

test("rankStories() is async and ranks by score, ties by id", async () => {
  const page = await site.open("/index.html");
  const out = await page.evaluate(async () => {
    const { rankStories, STORIES } = await import("/app.js");
    const pending = rankStories(STORIES.slice(0, 40));
    const isPromise = pending instanceof Promise;
    const ranked = await pending;
    const ties = await rankStories([
      { id: 2, title: "b", views: 5, comments: 0 },
      { id: 1, title: "a", views: 5, comments: 0 },
      { id: 3, title: "c", views: 1, comments: 1 },
    ]);
    return { isPromise, count: ranked.length, ids: ties.map((story) => story.id) };
  });
  assert.equal(out.isPromise, true, "rankStories() returns a Promise");
  assert.equal(out.count, 40);
  assert.deepEqual(out.ids, [3, 1, 2]);
});

test("sorting shows the right top 10", async () => {
  const page = await site.open("/index.html");
  await page.getByRole("button", { name: "Sort by popularity" }).click();
  await page.getByText("Sorted by popularity.").waitFor({ timeout: 5000 });
  assert.deepEqual(await page.locator("#stories li").allInnerTexts(), expectedTop10());
  assertNoPageErrors(page);
});

test("sorting never blocks the main thread for 50 ms or more (no long tasks)", async () => {
  const page = await site.open("/index.html");
  await page.waitForTimeout(600);
  await page.evaluate(() => {
    window.__longTasks = [];
    new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) window.__longTasks.push(Math.round(entry.duration));
    }).observe({ type: "longtask" });
  });
  await page.getByRole("button", { name: "Sort by popularity" }).click();
  await page.getByText("Sorted by popularity.").waitFor({ timeout: 5000 });
  await settle(page, 100);
  const longTasks = await page.evaluate(() => window.__longTasks);
  assert.deepEqual(longTasks, [], `long tasks of ${longTasks.join(", ")} ms: yield more often`);
});

test("axe-core finds no problems", async () => {
  const page = await site.open("/index.html");
  await page.waitForTimeout(600);
  await assertAccessible(page);
});
