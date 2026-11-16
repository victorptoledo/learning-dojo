import assert from "node:assert/strict";
import { test } from "node:test";
import { openSite, assertAccessible, assertValidHtml, box, computed } from "dojo-web";

const site = await openSite();
const near = (a, b, tolerance = 1.5) => Math.abs(a - b) <= tolerance;

/** Relative luminance (0 black .. 1 white) of a computed "rgb(r, g, b)" color. */
function luminance(color) {
  const [r, g, b] = (color.match(/[\d.]+/g) ?? []).slice(0, 3).map(Number).map((v) => {
    const c = v / 255;
    return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

test("the HTML is valid", async () => {
  await assertValidHtml("index.html");
});

test("the page has a header with the main nav, a main and a footer", async () => {
  const page = await site.open("/index.html");
  assert.equal(await page.getByRole("banner").count(), 1, "one <header> landmark");
  assert.equal(await page.getByRole("main").count(), 1, "one <main>");
  assert.equal(await page.getByRole("contentinfo").count(), 1, "one <footer> landmark");
  assert.equal(await page.getByRole("navigation", { name: "Main" }).count(), 1, 'a <nav aria-label="Main">');
  assert.ok((await page.locator('header nav[aria-label="Main"] a').count()) >= 3, "at least three links in the main nav");
});

test("a breadcrumb marks the current page", async () => {
  const page = await site.open("/index.html");
  const crumbs = page.getByRole("navigation", { name: "Breadcrumb" });
  assert.equal(await crumbs.count(), 1, 'a <nav aria-label="Breadcrumb">');
  assert.equal(await crumbs.locator("ol > li").count(), 3, "an <ol> with three items");
  const current = crumbs.locator('[aria-current="page"]');
  assert.equal(await current.count(), 1, 'the last item has aria-current="page"');
  assert.equal(await current.innerText(), "Trail Jacket");
});

test("the product is an article with a heading, a described photo and a machine-readable price", async () => {
  const page = await site.open("/index.html");
  const article = page.locator("main article.product");
  assert.equal(await article.count(), 1, "<article class=\"product\"> in main");
  assert.equal(await article.locator("h1").innerText(), "Trail Jacket");
  const photo = article.locator("img.product-photo");
  assert.ok(((await photo.getAttribute("alt")) ?? "").trim().length >= 10, "describe the photo in alt");
  assert.equal(await photo.getAttribute("width"), "800", "width and height attributes reserve the space");
  assert.equal(await photo.getAttribute("height"), "800");
  assert.equal(await article.locator(".price data").getAttribute("value"), "49.90", '<data value="49.90">');
  assert.match(await article.locator(".price data").innerText(), /49[.,]90/);
});

test("the specs are a description list", async () => {
  const page = await site.open("/index.html");
  const dts = page.locator("dl.specs > dt");
  const dds = page.locator("dl.specs > dd");
  assert.ok((await dts.count()) >= 3, "at least three <dt>");
  assert.equal(await dts.count(), await dds.count(), "one <dd> per <dt>");
});

test("the sizes are a labelled radio group, and a size is required", async () => {
  const page = await site.open("/index.html");
  const group = page.getByRole("group", { name: "Size" });
  assert.equal(await group.count(), 1, "a <fieldset> with <legend>Size</legend>");
  for (const size of ["S", "M", "L", "XL"]) {
    assert.equal(await group.getByRole("radio", { name: size, exact: true }).count(), 1, `a radio labelled ${size}`);
  }
  assert.equal(await page.locator('input[type="radio"][name="size"]').count(), 4, 'all four share name="size"');
  const form = page.locator("form.buy");
  assert.equal(await form.evaluate((f) => f.checkValidity()), false, "the form is invalid until a size is chosen");
  await group.getByRole("radio", { name: "M", exact: true }).check();
  assert.equal(await form.evaluate((f) => f.checkValidity()), true);
});

test("quantity is a labelled number from 1 to 10 and the button submits", async () => {
  const page = await site.open("/index.html");
  const quantity = page.getByLabel("Quantity");
  assert.equal(await quantity.getAttribute("type"), "number");
  assert.equal(await quantity.getAttribute("min"), "1");
  assert.equal(await quantity.getAttribute("max"), "10");
  assert.equal(await quantity.inputValue(), "1");
  const button = page.getByRole("button", { name: "Add to cart" });
  assert.equal(await button.getAttribute("type"), "submit");
});

test("on a phone the photo is above the details and fills the width", async () => {
  const page = await site.open("/index.html", { width: 375, height: 800 });
  const photo = await box(page, ".product-photo");
  const details = await box(page, ".product-details");
  const product = await box(page, ".product");
  assert.ok(details.y >= photo.y + photo.height, "the details come below the photo");
  assert.ok(near(photo.width, product.width), `the photo is ${photo.width}px of ${product.width}px`);
  assert.ok(near(photo.height, photo.width), "the photo keeps its square shape");
  const scroll = await page.evaluate(() => document.documentElement.scrollWidth);
  assert.ok(scroll <= 375, "no horizontal scrollbar");
});

test("from 56rem the photo and the details sit side by side in equal columns", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 900 });
  const photo = await box(page, ".product-photo");
  const details = await box(page, ".product-details");
  assert.ok(details.x >= photo.x + photo.width, "the details are right of the photo");
  assert.ok(near(photo.y, details.y), "both start at the same height");
  assert.ok(near(photo.width, details.width), `columns of ${photo.width}px and ${details.width}px`);
  const narrow = await site.open("/index.html", { width: 880, height: 900 });
  const below = await box(narrow, ".product-details");
  const above = await box(narrow, ".product-photo");
  assert.ok(below.y >= above.y + above.height, "below 56rem (896px) they are still stacked");
});

test("the size options are in one row on a desktop", async () => {
  const page = await site.open("/index.html", { width: 1280, height: 900 });
  const tops = await page.$$eval('input[name="size"]', (all) => all.map((input) => Math.round(input.getBoundingClientRect().top)));
  assert.equal(new Set(tops).size, 1, "the four sizes are on one line");
});

test("colors come from custom properties on :root", async () => {
  const page = await site.open("/index.html");
  const tokens = await page.evaluate(() => {
    const root = getComputedStyle(document.documentElement);
    return ["--color-bg", "--color-text", "--color-accent"].map((name) => root.getPropertyValue(name).trim());
  });
  tokens.forEach((value, i) => assert.notEqual(value, "", `${["--color-bg", "--color-text", "--color-accent"][i]} is defined`));
  const [button, accent] = await page.evaluate(() => {
    const probe = document.createElement("div");
    probe.style.color = "var(--color-accent)";
    document.body.append(probe);
    const resolved = getComputedStyle(probe).color;
    probe.remove();
    return [getComputedStyle(document.querySelector("form.buy button")).backgroundColor, resolved];
  });
  assert.equal(button, accent, "the button's background is var(--color-accent)");
});

test("the page follows a dark color scheme", async () => {
  const light = await site.open("/index.html", { colorScheme: "light" });
  const dark = await site.open("/index.html", { colorScheme: "dark" });
  const lightBody = await computed(light, "body", ["background-color", "color"]);
  const darkBody = await computed(dark, "body", ["background-color", "color"]);
  assert.ok(luminance(lightBody["background-color"]) > 0.7, "a light background in light mode");
  assert.ok(luminance(darkBody["background-color"]) < 0.1, `a dark background in dark mode (${darkBody["background-color"]})`);
  assert.ok(luminance(darkBody.color) > 0.6, "light text in dark mode");
});

test("axe-core finds no problems in light or dark mode", async () => {
  for (const colorScheme of ["light", "dark"]) {
    const page = await site.open("/index.html", { colorScheme });
    await assertAccessible(page);
  }
});

test("keyboard focus is visible", async () => {
  const page = await site.open("/index.html");
  await page.getByRole("radio", { name: "S", exact: true }).focus();
  await page.keyboard.press("Tab");
  const style = await computed(page, ":focus", ["outline-style", "outline-width", "box-shadow"]);
  const ring = (style["outline-style"] !== "none" && style["outline-width"] !== "0px") || style["box-shadow"] !== "none";
  assert.ok(ring, `the focused field has no visible focus ring (${JSON.stringify(style)})`);
});
