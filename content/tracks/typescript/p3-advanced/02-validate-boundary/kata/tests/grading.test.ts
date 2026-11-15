import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { configFromEnv, parseConfig, parseConfigJson, parseUserId } from "../config.ts";

const valid = {
  server: { host: "127.0.0.1", port: 3000 },
  database: { url: "postgres://localhost:5432/shop", poolSize: 5 },
  logLevel: "debug",
  features: ["search", "beta"],
};

/** The paths of the errors, without the messages (zod words them its own way). */
function errorPaths(result: { ok: true } | { ok: false; error: string[] }): string[] {
  assert.equal(result.ok, false, "expected the input to be rejected");
  if (result.ok) {
    return [];
  }
  return result.error.map((error) => error.slice(0, error.indexOf(":"))).sort();
}

describe("parseConfig", () => {
  test("accepts a complete configuration as it is", () => {
    assert.deepEqual(parseConfig(valid), { ok: true, value: valid });
  });

  test("drops unknown keys", () => {
    assert.deepEqual(parseConfig({ ...valid, extra: true }), { ok: true, value: valid });
  });

  test("rejects a bad port with its path", () => {
    for (const port of [0, 65536, 80.5, "80", -1]) {
      const result = parseConfig({ ...valid, server: { port } });
      assert.deepEqual(errorPaths(result), ["server.port"], String(port));
    }
  });

  test("errors look like path: message", () => {
    const result = parseConfig({ ...valid, logLevel: "loud" });
    assert.equal(result.ok, false);
    if (!result.ok) {
      assert.equal(result.error.length, 1);
      assert.match(result.error[0] ?? "", /^logLevel: .+/);
    }
  });

  test("reports every problem at once", () => {
    const result = parseConfig({
      server: { host: "", port: 3000 },
      database: { url: "not a url", poolSize: 0 },
      features: ["ok", 7],
    });
    assert.deepEqual(errorPaths(result), [
      "database.poolSize",
      "database.url",
      "features.1",
      "server.host",
    ]);
  });

  test("rejects what is not an object", () => {
    for (const input of [null, "config", 42, []]) {
      assert.equal(parseConfig(input).ok, false, JSON.stringify(input));
    }
    assert.deepEqual(errorPaths(parseConfig({})), ["database", "server"]);
  });
});

describe("parseConfigJson", () => {
  test("parses JSON text", () => {
    assert.deepEqual(parseConfigJson(JSON.stringify(valid)), { ok: true, value: valid });
  });

  test("reports text that is not JSON", () => {
    assert.deepEqual(parseConfigJson("{ port: 80 "), { ok: false, error: ["invalid JSON"] });
  });

  test("validates the parsed value", () => {
    assert.deepEqual(errorPaths(parseConfigJson('{"server":{"port":1}}')), ["database"]);
  });
});

describe("configFromEnv", () => {
  test("reads every variable", () => {
    assert.deepEqual(
      configFromEnv({
        APP_HOST: "localhost",
        APP_PORT: "8080",
        DATABASE_URL: "postgres://db/app",
        DB_POOL_SIZE: "20",
        LOG_LEVEL: "warn",
        FEATURES: "search, beta,,",
        HOME: "/home/dojo",
      }),
      {
        ok: true,
        value: {
          server: { host: "localhost", port: 8080 },
          database: { url: "postgres://db/app", poolSize: 20 },
          logLevel: "warn",
          features: ["search", "beta"],
        },
      },
    );
  });

  test("uses the defaults for missing optional variables", () => {
    assert.deepEqual(configFromEnv({ APP_PORT: "80", DATABASE_URL: "postgres://db/app" }), {
      ok: true,
      value: {
        server: { host: "0.0.0.0", port: 80 },
        database: { url: "postgres://db/app", poolSize: 10 },
        logLevel: "info",
        features: [],
      },
    });
  });

  test("rejects numbers that are not numbers", () => {
    const result = configFromEnv({ APP_PORT: "eighty", DATABASE_URL: "postgres://db/app", DB_POOL_SIZE: "" });
    assert.deepEqual(errorPaths(result), ["database.poolSize", "server.port"]);
  });

  test("reports missing required variables", () => {
    assert.deepEqual(errorPaths(configFromEnv({})), ["database.url", "server.port"]);
  });
});

describe("parseUserId", () => {
  test("accepts valid ids and rejects the rest", () => {
    assert.equal(parseUserId("usr_ab12cd34"), "usr_ab12cd34");
    for (const text of ["usr_AB12CD34", "usr_ab12", "user_ab12cd34", "usr_ab12cd345", ""]) {
      assert.equal(parseUserId(text), null, text);
    }
  });
});
