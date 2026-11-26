import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { createClient, defineFlags, FlagsError, parseOverrides } from "@dojo/flags";

const flags = defineFlags({
  darkMode: { kind: "boolean", default: false },
  maxUploads: { kind: "number", default: 3, min: 1, max: 10 },
  checkout: { kind: "variant", variants: ["classic", "one-page", "wizard"], default: "classic" },
});

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
const later = <T>(ms: number, value: T) => wait(ms).then(() => value);

const isFlagsError = (check: (error: FlagsError) => void) => (error: unknown) => {
  assert.ok(error instanceof FlagsError, `expected a FlagsError, got ${String(error)}`);
  assert.equal(error.name, "FlagsError");
  check(error);
  return true;
};

describe("defineFlags", () => {
  test("returns the definitions", () => {
    assert.equal(flags.checkout.default, "classic");
  });

  test("checks the definitions at runtime too", () => {
    const loose = defineFlags as (definitions: unknown) => unknown;
    assert.throws(() => loose({ size: { kind: "variant", variants: ["s", "m"], default: "xl" } }), FlagsError);
    assert.throws(() => loose({ limit: { kind: "number", default: 50, max: 10 } }), FlagsError);
    assert.throws(() => loose({ limit: { kind: "number", default: 5, min: 6 } }), FlagsError);
    assert.throws(() => loose({ "dark-mode": { kind: "boolean", default: true } }), FlagsError);
    assert.throws(() => loose({ Dark: { kind: "boolean", default: true } }), FlagsError);
  });
});

describe("parseOverrides", () => {
  test("accepts any subset of valid values", () => {
    assert.deepEqual(parseOverrides(flags, { checkout: "wizard", maxUploads: 10 }), {
      ok: true,
      overrides: { checkout: "wizard", maxUploads: 10 },
    });
    assert.deepEqual(parseOverrides(flags, {}), { ok: true, overrides: {} });
  });

  test("reports wrong types, ranges and variants with their keys", () => {
    const result = parseOverrides(flags, { darkMode: "yes", maxUploads: 11, checkout: "modal" });
    assert.equal(result.ok, false);
    const issues = result.ok ? [] : result.issues;
    for (const key of ["darkMode", "maxUploads", "checkout"]) {
      assert.ok(issues.some((issue) => issue.startsWith(`${key}: `)), `no issue for ${key}:\n${issues.join("\n")}`);
    }
  });

  test("reports each unknown key, and input that is not an object", () => {
    const result = parseOverrides(flags, { darkMode: true, colour: "red", beta: 1 });
    assert.equal(result.ok, false);
    const issues = result.ok ? [] : result.issues;
    assert.ok(issues.includes("colour: unknown flag"), issues.join("\n"));
    assert.ok(issues.includes("beta: unknown flag"), issues.join("\n"));
    const notObject = parseOverrides(flags, ["darkMode"]);
    assert.ok(!notObject.ok && notObject.issues.some((issue) => issue.startsWith("(root): ")));
  });
});

describe("FlagClient reads", () => {
  test("gives defaults, then overrides", () => {
    const client = createClient(flags, { overrides: { maxUploads: 7 } });
    assert.equal(client.get("darkMode"), false);
    assert.equal(client.get("maxUploads"), 7);
    assert.equal(client.get("checkout"), "classic");
  });

  test("rejects invalid initial overrides and unknown keys", () => {
    assert.throws(
      () => createClient(flags, { overrides: { maxUploads: 0 } }),
      isFlagsError((error) => assert.ok(error.issues.some((issue) => issue.startsWith("maxUploads: ")))),
    );
    const client = createClient(flags);
    const loose = client.get.bind(client) as (key: string) => unknown;
    assert.throws(() => loose("nope"), FlagsError);
    assert.throws(() => loose("toString"), FlagsError);
  });

  test("snapshot is a frozen copy", async () => {
    const client = createClient(flags);
    const before = client.snapshot();
    assert.deepEqual(before, { darkMode: false, maxUploads: 3, checkout: "classic" });
    assert.ok(Object.isFrozen(before));
    assert.throws(() => {
      (before as { darkMode: boolean }).darkMode = true;
    }, TypeError);
    await client.refresh(async () => ({ darkMode: true }));
    assert.equal(before.darkMode, false);
    assert.equal(client.snapshot().darkMode, true);
  });

  test("entries follow the order of the definitions", () => {
    const client = createClient(flags, { overrides: { checkout: "wizard" } });
    assert.deepEqual(
      [...client.entries()],
      [
        ["darkMode", false],
        ["maxUploads", 3],
        ["checkout", "wizard"],
      ],
    );
  });
});

describe("refresh", () => {
  test("replaces all overrides: missing keys go back to their default", async () => {
    const client = createClient(flags, { overrides: { darkMode: true, maxUploads: 5 } });
    const result = await client.refresh(async () => ({ maxUploads: 5, checkout: "one-page" }));
    assert.deepEqual(result, { changed: ["darkMode", "checkout"], stale: false });
    assert.deepEqual(client.snapshot(), { darkMode: false, maxUploads: 5, checkout: "one-page" });
  });

  test("calls the listeners of changed flags only, after the whole state changed", async () => {
    const client = createClient(flags);
    const calls: string[] = [];
    client.subscribe("darkMode", (value, previous) => {
      calls.push(`darkMode ${previous}->${value} checkout=${client.get("checkout")}`);
    });
    client.subscribe("maxUploads", () => calls.push("maxUploads"));
    const off = client.subscribe("checkout", () => calls.push("removed"));
    client.subscribe("checkout", (value) => calls.push(`checkout ${value}`));
    off();
    await client.refresh(async () => ({ darkMode: true, maxUploads: 3, checkout: "wizard" }));
    assert.deepEqual(calls, ["darkMode false->true checkout=wizard", "checkout wizard"]);
  });

  test("a failing listener does not stop the others and is reported with AggregateError", async () => {
    const client = createClient(flags);
    const calls: string[] = [];
    client.subscribe("darkMode", () => {
      throw new Error("listener broke");
    });
    client.subscribe("darkMode", () => calls.push("second"));
    client.subscribe("checkout", () => calls.push("checkout"));
    await assert.rejects(client.refresh(async () => ({ darkMode: true, checkout: "wizard" })), (error) => {
      assert.ok(error instanceof AggregateError);
      assert.equal(error.errors.length, 1);
      return true;
    });
    assert.deepEqual(calls, ["second", "checkout"]);
    assert.equal(client.get("darkMode"), true, "the state was applied before the listeners ran");
  });

  test("a failed load keeps the state and wraps the cause", async () => {
    const client = createClient(flags, { overrides: { darkMode: true } });
    let called = false;
    client.subscribe("darkMode", () => {
      called = true;
    });
    const cause = new Error("503");
    await assert.rejects(
      client.refresh(async () => {
        throw cause;
      }),
      isFlagsError((error) => assert.equal(error.cause, cause)),
    );
    assert.equal(client.get("darkMode"), true);
    assert.equal(called, false);
  });

  test("an invalid payload changes nothing, not even its valid keys", async () => {
    const client = createClient(flags);
    await assert.rejects(
      client.refresh(async () => ({ darkMode: true, maxUploads: 99 })),
      isFlagsError((error) => assert.ok(error.issues.some((issue) => issue.startsWith("maxUploads: ")))),
    );
    assert.deepEqual(client.snapshot(), { darkMode: false, maxUploads: 3, checkout: "classic" });
  });

  test("times out, aborting the signal given to load", async () => {
    const client = createClient(flags);
    let given: AbortSignal | undefined;
    const started = Date.now();
    await assert.rejects(
      client.refresh(
        (signal) => {
          given = signal;
          return new Promise(() => {}); // never settles and ignores the signal
        },
        { timeoutMs: 40 },
      ),
      isFlagsError((error) => assert.equal((error.cause as Error).name, "TimeoutError")),
    );
    assert.ok(Date.now() - started < 500);
    assert.equal(given?.aborted, true);
  });

  test("a refresh that finishes after a newer one started is stale", async () => {
    const client = createClient(flags);
    const slow = client.refresh(() => later(60, { checkout: "wizard" }));
    const fast = client.refresh(() => later(5, { checkout: "one-page" }));
    assert.deepEqual(await fast, { changed: ["checkout"], stale: false });
    assert.deepEqual(await slow, { changed: [], stale: true });
    assert.equal(client.get("checkout"), "one-page");
  });
});

describe("package boundary", () => {
  test("only the package's main entry point can be imported", async () => {
    const internal = "@dojo/flags/client.ts";
    await assert.rejects(import(internal), { code: "ERR_PACKAGE_PATH_NOT_EXPORTED" });
    const deep = "@dojo/flags/src/client.ts";
    await assert.rejects(import(deep), { code: "ERR_PACKAGE_PATH_NOT_EXPORTED" });
  });
});
