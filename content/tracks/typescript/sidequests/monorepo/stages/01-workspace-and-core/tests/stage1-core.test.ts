import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { add, formatMoney, isCurrency, money, parseMoney, sum, type Money } from "@tally/core";
import { field, isRecord, readJson } from "./repo.ts";

const cents = (text: string): number => {
  const parsed = parseMoney(text);
  assert.ok(parsed.ok, `${text}: ${parsed.ok ? "" : parsed.error}`);
  return parsed.ok ? parsed.value.cents : NaN;
};

describe("the workspace", () => {
  test("the root package is private and lists packages/* as workspaces", () => {
    const root = readJson("package.json");
    assert.equal(field(root, "private"), true);
    const workspaces = field(root, "workspaces");
    assert.ok(Array.isArray(workspaces) && workspaces.includes("packages/*"), "workspaces must include packages/*");
  });

  test("@tally/core is an ES module whose exports point at src/index.ts, without wildcards", () => {
    const core = readJson("packages", "core", "package.json");
    assert.equal(field(core, "name"), "@tally/core");
    assert.equal(typeof field(core, "version"), "string");
    assert.equal(field(core, "type"), "module");
    const exports = field(core, "exports");
    assert.ok(isRecord(exports), "exports must be an object of subpaths");
    assert.equal(exports["."], "./src/index.ts");
    for (const key of Object.keys(exports)) {
      assert.ok(!key.includes("*"), `exports must not have wildcards: ${key}`);
    }
  });
});

describe("money", () => {
  test("keeps cents and currency", () => {
    const value = money(1250, "BRL");
    assert.equal(value.cents, 1250);
    assert.equal(value.currency, "BRL");
  });

  test("refuses cents that are not a safe integer", () => {
    for (const bad of [1.5, Number.NaN, Number.POSITIVE_INFINITY, 2 ** 60]) {
      assert.throws(() => money(bad, "USD"), RangeError, String(bad));
    }
  });

  test("isCurrency is a guard for the three currencies", () => {
    assert.deepEqual(["BRL", "USD", "EUR", "brl", "GBP", 1, null].map(isCurrency), [true, true, true, false, false, false, false]);
  });
});

describe("parseMoney", () => {
  test("reads cents exactly", () => {
    assert.equal(cents("12 BRL"), 1200);
    assert.equal(cents("12.5 BRL"), 1250);
    assert.equal(cents("12.34 USD"), 1234);
    assert.equal(cents("-0.05 EUR"), -5);
    assert.equal(cents("0.29 BRL"), 29);
    assert.equal(cents("1.10 BRL"), 110);
    assert.equal(cents("  7.01   USD "), 701);
    assert.equal(cents("0 BRL"), 0);
  });

  test("keeps the currency", () => {
    const parsed = parseMoney("3 EUR");
    assert.ok(parsed.ok && parsed.value.currency === "EUR");
  });

  test("reports what is wrong, in order", () => {
    assert.deepEqual(parseMoney("12"), { ok: false, error: "invalid money: 12" });
    assert.deepEqual(parseMoney("12 BRL extra"), { ok: false, error: "invalid money: 12 BRL extra" });
    assert.deepEqual(parseMoney(""), { ok: false, error: "invalid money: " });
    assert.deepEqual(parseMoney("12 brl"), { ok: false, error: "unknown currency: brl" });
    assert.deepEqual(parseMoney("abc GBP"), { ok: false, error: "unknown currency: GBP" });
    for (const amount of ["12.345", "1,50", ".5", "5.", "+5", "1e3", "abc", "--1"]) {
      assert.deepEqual(parseMoney(`${amount} BRL`), { ok: false, error: `invalid amount: ${amount}` });
    }
    assert.deepEqual(parseMoney("99999999999999999999 USD"), { ok: false, error: "invalid amount: 99999999999999999999" });
  });
});

describe("formatMoney", () => {
  test("two decimals, a minus sign and the currency", () => {
    assert.equal(formatMoney(money(1250, "BRL")), "12.50 BRL");
    assert.equal(formatMoney(money(-5, "USD")), "-0.05 USD");
    assert.equal(formatMoney(money(123400, "EUR")), "1234.00 EUR");
    assert.equal(formatMoney(money(0, "BRL")), "0.00 BRL");
    assert.equal(formatMoney(money(-123456, "BRL")), "-1234.56 BRL");
  });

  test("round-trips with parseMoney", () => {
    for (const text of ["0.01 BRL", "-99.99 USD", "100.00 EUR"]) {
      const parsed = parseMoney(text);
      assert.ok(parsed.ok);
      assert.equal(parsed.ok && formatMoney(parsed.value), text);
    }
  });
});

describe("add and sum", () => {
  test("add the cents of one currency", () => {
    assert.equal(add(money(10, "BRL"), money(-3, "BRL")).cents, 7);
    const total: Money = sum([money(1, "USD"), money(2, "USD"), money(3, "USD")], "USD");
    assert.equal(total.cents, 6);
    assert.equal(total.currency, "USD");
  });

  test("an empty sum is zero of that currency", () => {
    const zero = sum([], "EUR");
    assert.equal(zero.cents, 0);
    assert.equal(zero.currency, "EUR");
  });

  test("currencies do not mix", () => {
    assert.throws(() => add(money(1, "BRL"), money(1, "USD")), { name: "TypeError", message: "cannot add BRL and USD" });
    assert.throws(() => sum([money(1, "BRL"), money(1, "EUR")], "BRL"), { name: "TypeError", message: "cannot add BRL and EUR" });
    assert.throws(() => sum([money(1, "USD")], "BRL"), { name: "TypeError", message: "cannot add BRL and USD" });
  });

  test("0.1 + 0.2 is exactly 0.3", () => {
    const a = parseMoney("0.1 BRL");
    const b = parseMoney("0.2 BRL");
    assert.ok(a.ok && b.ok);
    if (a.ok && b.ok) {
      assert.equal(formatMoney(add(a.value, b.value)), "0.30 BRL");
    }
  });
});
