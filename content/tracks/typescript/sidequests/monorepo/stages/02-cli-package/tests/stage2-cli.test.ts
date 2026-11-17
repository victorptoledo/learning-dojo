import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { run, type Io } from "@tally/cli";
import { boundaryViolations, field, isRecord, readJson, sourcesUnder } from "./repo.ts";

/** Runs the CLI on files held in memory and returns what it did. */
function tally(argv: string[], files: Record<string, string> = {}) {
  const out: string[] = [];
  const err: string[] = [];
  const io: Io = {
    readFile: (path) => {
      const text = files[path];
      if (text === undefined) {
        throw new Error(`ENOENT: ${path}`);
      }
      return text;
    },
    out: (line) => out.push(line),
    err: (line) => err.push(line),
  };
  const code = run(argv, io);
  return { code, out, err };
}

const LEDGER = [
  "# November",
  "2026-11-01  12.50 BRL  coffee with Ana",
  "2026-11-03  4.99 USD   app subscription",
  "",
  "2026-11-10  30 BRL     lunch",
  "2026-10-31  1.01 EUR   old ticket",
  "2026-10-30  -2 BRL     refund",
].join("\n");

describe("the package", () => {
  test("@tally/cli exports src/index.ts and depends on @tally/core", () => {
    const cli = readJson("packages", "cli", "package.json");
    assert.equal(field(cli, "name"), "@tally/cli");
    assert.equal(field(cli, "type"), "module");
    assert.equal(field(cli, "exports", "."), "./src/index.ts");
    const coreVersion = field(readJson("packages", "core", "package.json"), "version");
    assert.equal(field(cli, "dependencies", "@tally/core"), `^${String(coreVersion)}`);
  });

  test("the CLI reaches core only through @tally/core", () => {
    assert.ok(sourcesUnder("packages", "cli", "src").length > 0, "packages/cli/src has no .ts files");
    assert.deepEqual(boundaryViolations("packages/cli", ["@tally/core"]), []);
  });

  test("core does not depend on the CLI", () => {
    assert.deepEqual(boundaryViolations("packages/core", []), []);
    const dependencies = field(readJson("packages", "core", "package.json"), "dependencies");
    assert.ok(dependencies === undefined || (isRecord(dependencies) && !("@tally/cli" in dependencies)));
  });
});

describe("tally sum", () => {
  test("prints the totals per currency, BRL then USD then EUR", () => {
    assert.deepEqual(tally(["sum", "ledger.txt"], { "ledger.txt": LEDGER }), {
      code: 0,
      out: ["40.50 BRL", "4.99 USD", "1.01 EUR"],
      err: [],
    });
  });

  test("only the currencies that appear", () => {
    assert.deepEqual(tally(["sum", "l"], { l: "2026-01-01 3 EUR a\n2026-01-02 0.5 EUR b" }).out, ["3.50 EUR"]);
  });

  test("--month keeps one month", () => {
    assert.deepEqual(tally(["sum", "ledger.txt", "--month", "2026-10"], { "ledger.txt": LEDGER }).out, ["-2.00 BRL", "1.01 EUR"]);
    assert.deepEqual(tally(["sum", "ledger.txt", "--month", "2026-11"], { "ledger.txt": LEDGER }).out, ["42.50 BRL", "4.99 USD"]);
  });

  test("no expenses", () => {
    assert.deepEqual(tally(["sum", "e"], { e: "" }), { code: 0, out: ["no expenses"], err: [] });
    assert.deepEqual(tally(["sum", "e"], { e: "# nothing yet\n\n" }).out, ["no expenses"]);
    assert.deepEqual(tally(["sum", "ledger.txt", "--month", "2025-01"], { "ledger.txt": LEDGER }).out, ["no expenses"]);
  });

  test("handles Windows line endings and extra spaces", () => {
    assert.deepEqual(tally(["sum", "w"], { w: "  2026-11-01\t1 BRL\tbus  \r\n2026-11-02 2 BRL taxi\r\n" }).out, ["3.00 BRL"]);
  });
});

describe("errors", () => {
  test("usage problems exit 2", () => {
    const files = { l: LEDGER };
    for (const argv of [[], ["total", "l"], ["sum"], ["sum", "--month", "2026-11"], ["sum", "l", "--year", "2026"], ["sum", "l", "--month"], ["sum", "l", "--month", "2026-1"], ["sum", "l", "--month", "2026-11", "x"]]) {
      assert.deepEqual(tally(argv, files), { code: 2, out: [], err: ["usage: tally sum <file> [--month YYYY-MM]"] }, argv.join(" "));
    }
  });

  test("a file that cannot be read exits 1", () => {
    assert.deepEqual(tally(["sum", "missing.txt"]), { code: 1, out: [], err: ["error: cannot read missing.txt"] });
  });

  test("the first wrong line is reported with its number", () => {
    const cases: [string, string][] = [
      ["2026-11-01 1 BRL ok\n# c\n11/02/2026 1 BRL bad date", "line 3: invalid date: 11/02/2026"],
      ["2026-11-01 1,50 BRL x", "line 1: invalid amount: 1,50"],
      ["2026-11-01 1 GBP tea", "line 1: unknown currency: GBP"],
      ["2026-11-01 1 BRL", "line 1: missing description"],
      ["\n\nnot-a-date", "line 3: invalid date: not-a-date"],
      ["2026-11-01 x BRL a\n2026-11-02 y BRL b", "line 1: invalid amount: x"],
    ];
    for (const [text, message] of cases) {
      const result = tally(["sum", "l"], { l: text });
      assert.equal(result.code, 1, text);
      assert.deepEqual(result.out, [], text);
      assert.deepEqual(result.err, [message], text);
    }
  });
});
