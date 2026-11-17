import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { createHandler } from "@tally/api";
import { moneyOf } from "@tally/core/testing";
import { boundaryViolations, field, importsOf, isRecord, readJson, sourcesUnder } from "./repo.ts";

const post = (body: unknown) => ({ method: "POST", path: "/expenses", body });

describe("the test kit", () => {
  test("core exports exactly . and ./testing", () => {
    const exports = field(readJson("packages", "core", "package.json"), "exports");
    assert.ok(isRecord(exports));
    assert.deepEqual(Object.keys(exports).sort(), [".", "./testing"]);
    assert.equal(exports["."], "./src/index.ts");
  });

  test("moneyOf parses or throws the parse error", () => {
    const value = moneyOf("12.50 BRL");
    assert.equal(value.cents, 1250);
    assert.equal(value.currency, "BRL");
    assert.throws(() => moneyOf("12 GBP"), { message: "unknown currency: GBP" });
  });

  test("the main entry does not export the test kit", async () => {
    const core: Record<string, unknown> = await import("@tally/core");
    assert.equal("moneyOf" in core, false);
  });

  test("no production code imports the test kit", () => {
    const offenders = sourcesUnder("packages")
      .filter((file) => file.includes("/src/") && !file.startsWith("packages/core/"))
      .filter((file) => importsOf(file).some((specifier) => specifier === "@tally/core/testing"));
    assert.deepEqual(offenders, []);
  });
});

describe("the api package", () => {
  test("@tally/api exports src/index.ts and depends on core only", () => {
    const api = readJson("packages", "api", "package.json");
    assert.equal(field(api, "name"), "@tally/api");
    assert.equal(field(api, "type"), "module");
    assert.equal(field(api, "exports", "."), "./src/index.ts");
    const coreVersion = String(field(readJson("packages", "core", "package.json"), "version"));
    assert.deepEqual(field(api, "dependencies"), { "@tally/core": `^${coreVersion}` });
  });

  test("reaches core only through @tally/core, and never the CLI", () => {
    assert.ok(sourcesUnder("packages", "api", "src").length > 0);
    assert.deepEqual(boundaryViolations("packages/api", ["@tally/core"]), []);
  });
});

describe("the handler", () => {
  test("creates, lists and reads expenses", () => {
    const handle = createHandler();
    assert.deepEqual(handle(post({ date: "2026-11-01", amount: "12.5 BRL", description: "  coffee " })), {
      status: 201,
      body: { id: "1", date: "2026-11-01", amount: "12.50 BRL", description: "coffee" },
    });
    handle(post({ date: "2026-11-02", amount: "3 USD", description: "app" }));
    assert.deepEqual(handle({ method: "GET", path: "/expenses" }), {
      status: 200,
      body: [
        { id: "1", date: "2026-11-01", amount: "12.50 BRL", description: "coffee" },
        { id: "2", date: "2026-11-02", amount: "3.00 USD", description: "app" },
      ],
    });
    assert.deepEqual(handle({ method: "GET", path: "/expenses/2" }), {
      status: 200,
      body: { id: "2", date: "2026-11-02", amount: "3.00 USD", description: "app" },
    });
  });

  test("totals per currency in order", () => {
    const handle = createHandler();
    assert.deepEqual(handle({ method: "GET", path: "/totals" }), { status: 200, body: {} });
    handle(post({ date: "2026-11-01", amount: "1 EUR", description: "a" }));
    handle(post({ date: "2026-11-01", amount: "2.25 BRL", description: "b" }));
    handle(post({ date: "2026-11-01", amount: "0.75 BRL", description: "c" }));
    const totals = handle({ method: "GET", path: "/totals" });
    assert.deepEqual(totals, { status: 200, body: { BRL: "3.00 BRL", EUR: "1.00 EUR" } });
    assert.deepEqual(Object.keys(totals.body as object), ["BRL", "EUR"]);
  });

  test("deletes, and ids are never reused", () => {
    const handle = createHandler();
    handle(post({ date: "2026-11-01", amount: "1 BRL", description: "a" }));
    handle(post({ date: "2026-11-01", amount: "2 BRL", description: "b" }));
    assert.deepEqual(handle({ method: "DELETE", path: "/expenses/2" }), { status: 204, body: null });
    assert.deepEqual(handle({ method: "DELETE", path: "/expenses/2" }), { status: 404, body: { error: "no expense 2" } });
    assert.equal(handle(post({ date: "2026-11-01", amount: "3 BRL", description: "c" })).status, 201);
    const list = handle({ method: "GET", path: "/expenses" }).body;
    assert.deepEqual(Array.isArray(list) && list.map((expense: { id: string }) => expense.id), ["1", "3"]);
  });

  test("two handlers do not share expenses", () => {
    const first = createHandler();
    const second = createHandler();
    first(post({ date: "2026-11-01", amount: "1 BRL", description: "a" }));
    assert.deepEqual(second({ method: "GET", path: "/expenses" }), { status: 200, body: [] });
    assert.equal((second(post({ date: "2026-11-01", amount: "1 BRL", description: "a" })).body as { id: string }).id, "1");
  });

  test("404 and 405", () => {
    const handle = createHandler();
    handle(post({ date: "2026-11-01", amount: "1 BRL", description: "a" }));
    assert.deepEqual(handle({ method: "GET", path: "/expenses/9" }), { status: 404, body: { error: "no expense 9" } });
    assert.deepEqual(handle({ method: "GET", path: "/nope" }), { status: 404, body: { error: "not found" } });
    assert.deepEqual(handle({ method: "GET", path: "/" }), { status: 404, body: { error: "not found" } });
    assert.deepEqual(handle({ method: "PUT", path: "/expenses" }), { status: 405, body: { error: "method not allowed" } });
    assert.deepEqual(handle({ method: "PUT", path: "/expenses/1" }), { status: 405, body: { error: "method not allowed" } });
    assert.deepEqual(handle({ method: "POST", path: "/totals" }), { status: 405, body: { error: "method not allowed" } });
  });

  test("invalid expenses are 400 with the first problem", () => {
    const handle = createHandler();
    const cases: [unknown, string][] = [
      [null, "body must be an object"],
      [[1], "body must be an object"],
      ["text", "body must be an object"],
      [{ amount: "1 BRL", description: "a" }, "invalid date: undefined"],
      [{ date: "01/11/2026", amount: "x", description: "" }, "invalid date: 01/11/2026"],
      [{ date: "2026-11-01", amount: 12, description: "a" }, "amount must be text"],
      [{ date: "2026-11-01", amount: "12 GBP", description: "a" }, "unknown currency: GBP"],
      [{ date: "2026-11-01", amount: "1.234 BRL", description: "a" }, "invalid amount: 1.234"],
      [{ date: "2026-11-01", amount: "1 BRL", description: "   " }, "missing description"],
      [{ date: "2026-11-01", amount: "1 BRL" }, "missing description"],
    ];
    for (const [body, message] of cases) {
      assert.deepEqual(handle(post(body)), { status: 400, body: { error: message } }, JSON.stringify(body));
    }
    assert.deepEqual(handle({ method: "GET", path: "/expenses" }), { status: 200, body: [] });
  });
});
