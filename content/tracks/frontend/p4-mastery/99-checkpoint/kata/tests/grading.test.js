import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { openSite, assertAccessible, assertNoPageErrors, settle } from "dojo-web";

// How the fake payment provider answers: "ok", "down" (500) or "slow" (1.5 s, after the timeout
// when the test runs the page's fake clock forward).
let payMode = "ok";
const PAY_SCRIPT = readFileSync("vendor/pay.js");
const site = await openSite({
  routes: {
    "POST /api/errors": () => ({ status: 204 }),
    "GET /vendor/pay.js": () => {
      if (payMode === "down") {
        return { status: 500, body: "provider down" };
      }
      return {
        headers: { "content-type": "text/javascript; charset=utf-8" },
        body: PAY_SCRIPT,
        delayMs: payMode === "slow" ? 1500 : 0,
      };
    },
  },
});

const reports = () =>
  site.requests
    .filter((request) => request.method === "POST" && request.path === "/api/errors")
    .map((request) => JSON.parse(request.body));

async function waitFor(check, what, ms = 2000) {
  const end = Date.now() + ms;
  while (Date.now() < end) {
    if (check()) return;
    await new Promise((resolve) => setTimeout(resolve, 25));
  }
  assert.fail(`timed out waiting for ${what}`);
}

const increase = (page, item) => page.getByRole("button", { name: `Increase ${item} quantity` });
const decrease = (page, item) => page.getByRole("button", { name: `Decrease ${item} quantity` });
const quantity = (page, item) => page.getByRole("spinbutton", { name: `${item} quantity` });

async function opened(path = "/index.html", options = {}) {
  payMode = options.payMode ?? "ok";
  const page = await site.open(path, options);
  if (!options.clock && options.javaScriptEnabled !== false) {
    await page.locator("#payment .pay-button, #bank-transfer:not([hidden])").first().waitFor();
  }
  return page;
}

test("without JavaScript the cart is a plain form that still works", async () => {
  const page = await opened("/index.html", { javaScriptEnabled: false });
  assert.equal(await quantity(page, "Dojo Tee").inputValue(), "1");
  assert.equal(await page.getByRole("button", { name: "Update cart" }).isVisible(), true);
  assert.equal(await page.locator("quantity-input button").count(), 0);
  assert.equal(await page.locator("#bank-transfer").isVisible(), true, "bank transfer needs no script");
  assert.equal(await page.locator("#total").innerText(), "€54.00");
});

test("with JavaScript each quantity gets − and + buttons named after its item", async () => {
  const page = await opened();
  for (const item of ["Dojo Tee", "Belt Mug"]) {
    assert.equal(await decrease(page, item).count(), 1, `Decrease ${item} quantity`);
    assert.equal(await increase(page, item).count(), 1, `Increase ${item} quantity`);
  }
  assert.equal(await page.locator("quantity-input button").count(), 4);
  assert.equal(await page.locator('quantity-input button[type="button"]').count(), 4, "type=button, so they do not submit the form");
  assert.equal(await page.getByRole("button", { name: "Update cart" }).isHidden(), true, "the total updates live: hide Update cart");
  assert.ok(await page.evaluate(() => customElements.get("quantity-input")?.name === "QuantityInput"));
});

test("the buttons step the quantity and stop at min and max", async () => {
  const page = await opened();
  await increase(page, "Dojo Tee").click();
  await increase(page, "Dojo Tee").click();
  assert.equal(await quantity(page, "Dojo Tee").inputValue(), "3");
  await decrease(page, "Dojo Tee").click();
  await decrease(page, "Dojo Tee").click();
  await decrease(page, "Dojo Tee").click();
  assert.equal(await quantity(page, "Dojo Tee").inputValue(), "0");
  assert.equal(await decrease(page, "Dojo Tee").isDisabled(), true, "− is disabled at min");
  assert.equal(await increase(page, "Dojo Tee").isDisabled(), false);
  await quantity(page, "Belt Mug").fill("10");
  await quantity(page, "Belt Mug").blur();
  await settle(page);
  assert.equal(await increase(page, "Belt Mug").isDisabled(), true, "+ is disabled at max");
});

test("typed quantities are clamped to the input's min and max", async () => {
  const page = await opened();
  const cases = [["25", "10"], ["-3", "0"], ["", "0"], ["4", "4"]];
  for (const [typed, expected] of cases) {
    await quantity(page, "Belt Mug").fill(typed);
    await quantity(page, "Belt Mug").blur();
    await settle(page);
    assert.equal(await quantity(page, "Belt Mug").inputValue(), expected, `typed "${typed}"`);
  }
});

test("a change sends one bubbling quantity-change event with the name and the value", async () => {
  const page = await opened();
  await page.evaluate(() => {
    window.changes = [];
    document.addEventListener("quantity-change", (event) => window.changes.push(event.detail));
  });
  await increase(page, "Dojo Tee").click();
  await quantity(page, "Belt Mug").fill("7");
  await quantity(page, "Belt Mug").blur();
  await quantity(page, "Belt Mug").fill("12");
  await quantity(page, "Belt Mug").blur();
  // 15 is clamped to 10, the value it already has: nothing changed.
  await quantity(page, "Belt Mug").fill("15");
  await quantity(page, "Belt Mug").blur();
  await settle(page);
  assert.deepEqual(await page.evaluate(() => window.changes), [
    { name: "qty-tee", value: 2 },
    { name: "qty-mug", value: 7 },
    { name: "qty-mug", value: 10 },
  ], "no event when the value stays the same");
});

test("the value property reads and clamps the quantity without an event", async () => {
  const page = await opened();
  const out = await page.evaluate(() => {
    const element = document.querySelector("quantity-input");
    let events = 0;
    element.addEventListener("quantity-change", () => events++);
    const before = element.value;
    element.value = 99;
    const after = element.value;
    return { before, after, input: element.querySelector("input").value, events, plus: element.querySelector("button:last-of-type").disabled };
  });
  assert.deepEqual(out, { before: 1, after: 10, input: "10", events: 0, plus: true });
});

test("moving the element does not add the buttons again", async () => {
  const page = await opened();
  const count = await page.evaluate(() => {
    const element = document.querySelector("quantity-input");
    const items = document.querySelector(".items");
    items.append(element.closest(".item"));
    items.prepend(element.closest(".item"));
    return element.querySelectorAll("button").length;
  });
  assert.equal(count, 2);
  await increase(page, "Dojo Tee").click();
  assert.equal(await quantity(page, "Dojo Tee").inputValue(), "2", "one click is still one step");
});

test("formatPrice() uses Intl for each locale's currency format", async () => {
  const page = await opened();
  const out = await page.evaluate(async () => {
    const { formatPrice } = await import("/app.js");
    return [
      formatPrice(5400, "EUR", "en-IE"),
      formatPrice(123456, "EUR", "de-DE"),
      formatPrice(5, "USD", "en-US"),
      formatPrice(2900, "BRL", "pt-BR"),
    ];
  });
  assert.deepEqual(out, ["€54.00", "1.234,56 €", "$0.05", "R$ 29,00"]);
});

test("itemsLabel() picks the plural with Intl.PluralRules and formats the number", async () => {
  const page = await opened();
  const out = await page.evaluate(async () => {
    const { itemsLabel } = await import("/app.js");
    return [itemsLabel(1, "en-IE"), itemsLabel(0, "en-IE"), itemsLabel(3, "en-IE"), itemsLabel(1000, "en-IE"), itemsLabel(1, "pt-BR"), itemsLabel(2, "pt-BR"), itemsLabel(2, "fr-FR")];
  });
  assert.deepEqual(out, ["1 item", "0 items", "3 items", "1,000 items", "1 item", "2 itens", "2 items"]);
});

test("the count and the total follow every change", async () => {
  const page = await opened();
  await increase(page, "Dojo Tee").click();
  assert.equal(await page.locator("#total").innerText(), "€83.00");
  assert.equal(await page.locator("#count").innerText(), "4 items");
  await quantity(page, "Belt Mug").fill("0");
  await quantity(page, "Belt Mug").blur();
  await decrease(page, "Dojo Tee").click();
  assert.equal(await page.locator("#total").innerText(), "€29.00");
  assert.equal(await page.locator("#count").innerText(), "1 item");
});

test("uncaught errors and unhandled rejections are reported with the release", async () => {
  const before = reports().length;
  const page = await opened();
  await page.evaluate(() => {
    setTimeout(() => {
      throw new Error("boom in a timer");
    }, 0);
    Promise.reject(new Error("lost promise"));
  });
  await waitFor(() => reports().length >= before + 2, "two reports on POST /api/errors");
  const sent = reports().slice(before);
  const boom = sent.find((report) => report.message.includes("boom in a timer"));
  const lost = sent.find((report) => report.message.includes("lost promise"));
  assert.ok(boom && lost, `reports: ${JSON.stringify(sent)}`);
  assert.deepEqual([boom.kind, boom.release, boom.page], ["error", "2026.11.26-3", "/index.html"]);
  assert.deepEqual([lost.kind, lost.release], ["rejection", "2026.11.26-3"]);
});

test("a repeated error is reported once, and a page sends at most 5 reports", async () => {
  const page = await opened();
  const before = reports().length;
  const sent = await page.evaluate(async () => {
    const { reportProblem } = await import("/app.js");
    const results = [reportProblem("error", "same"), reportProblem("error", "same")];
    for (let i = 0; i < 8; i++) results.push(reportProblem("error", `distinct ${i}`));
    return results.filter(Boolean).length;
  });
  assert.equal(sent, 5, "reportProblem returns true only for the reports it sent");
  await settle(page, 300);
  assert.equal(reports().length - before, 5);
});

test("when the payment script loads, the card button is mounted", async () => {
  const page = await opened("/index.html", { payMode: "ok" });
  await page.getByRole("button", { name: "Pay with card" }).waitFor();
  assert.equal(await page.locator("#bank-transfer").isHidden(), true, "bank transfer is the fallback only");
  assert.equal(await page.locator("#payment-status").innerText(), "");
  assertNoPageErrors(page);
});

test("when the payment provider is down, the page falls back to bank transfer and reports it", async () => {
  const before = reports().length;
  const page = await opened("/index.html", { payMode: "down" });
  await page.getByText("Card payments are unavailable right now.").waitFor();
  assert.equal(await page.locator("#bank-transfer").isVisible(), true);
  await waitFor(() => reports().slice(before).some((report) => report.kind === "payment"), "a payment report");
  assert.equal(await page.getByRole("button", { name: "Pay with card" }).count(), 0);
});

test("a payment script slower than 3 s is given up, and its late arrival is ignored", async () => {
  // A pending script holds back the load event, so the page counts as open once its DOM is ready.
  const page = await opened("/index.html", { payMode: "slow", clock: true, waitUntil: "domcontentloaded" });
  assert.equal(await page.locator("#payment-status").innerText(), "Loading card payments…");
  await page.clock.runFor(2500);
  await settle(page);
  assert.equal(await page.locator("#bank-transfer").isHidden(), true, "too early to give up");
  await page.clock.runFor(600);
  await page.getByText("Card payments are unavailable right now.").waitFor();
  assert.equal(await page.locator("#bank-transfer").isVisible(), true);
  await page.waitForTimeout(1800); // the script arrives now
  assert.equal(await page.getByRole("button", { name: "Pay with card" }).count(), 0, "do not mount after giving up");
});

test("axe-core finds no problems", async () => {
  const page = await opened();
  await increase(page, "Belt Mug").click();
  await assertAccessible(page);
});
