import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { describe, test } from "node:test";
import { fileURLToPath } from "node:url";
import { greeting, main, parseOptions, USAGE, type Io } from "../cli.ts";

function fakeIo(env: Record<string, string> = {}) {
  const out: string[] = [];
  const err: string[] = [];
  const io: Io = { env, stdout: (line) => out.push(line), stderr: (line) => err.push(line) };
  return { io, out, err };
}

describe("greeting", () => {
  test("speaks three languages", () => {
    assert.equal(greeting("Ada", "en"), "Hello, Ada!");
    assert.equal(greeting("Ada", "pt"), "Olá, Ada!");
    assert.equal(greeting("Ada", "es"), "Hola, Ada!");
  });
});

describe("parseOptions", () => {
  test("reads long and short options", () => {
    const parsed = parseOptions(["-l", "pt", "--times", "3", "-s", "Ada"]);
    assert.deepEqual(parsed, {
      ok: true,
      options: { names: ["Ada"], times: 3, shout: true, lang: "pt", help: false },
    });
  });

  test("uses the defaults", () => {
    assert.deepEqual(parseOptions(["Ada"]), {
      ok: true,
      options: { names: ["Ada"], times: 1, shout: false, lang: "en", help: false },
    });
  });

  test("--lang wins over the default language, which wins over en", () => {
    const fromFlag = parseOptions(["--lang", "es", "Ada"], "pt");
    const fromDefault = parseOptions(["Ada"], "pt");
    assert.ok(fromFlag.ok && fromFlag.options.lang === "es");
    assert.ok(fromDefault.ok && fromDefault.options.lang === "pt");
  });

  test("rejects unknown options, bad numbers, unknown languages and no names", () => {
    for (const argv of [
      ["--loud", "Ada"],
      ["--times", "0", "Ada"],
      ["--times", "6", "Ada"],
      ["--times", "1.5", "Ada"],
      ["--times", "two", "Ada"],
      ["--lang", "fr", "Ada"],
      ["--lang"],
      [],
    ]) {
      const parsed = parseOptions(argv);
      assert.equal(parsed.ok, false, `expected an error for ${JSON.stringify(argv)}`);
      assert.ok(!parsed.ok && parsed.error.length > 0);
    }
  });

  test("--help needs no name", () => {
    const parsed = parseOptions(["--help"]);
    assert.ok(parsed.ok && parsed.options.help);
  });
});

describe("main", () => {
  test("repeats, shouts and reads HELLO_LANG", () => {
    const { io, out } = fakeIo({ HELLO_LANG: "pt" });
    assert.equal(main(["-t", "2", "--shout", "Ada", "Linus"], io), 0);
    assert.deepEqual(out, ["OLÁ, ADA!", "OLÁ, LINUS!", "OLÁ, ADA!", "OLÁ, LINUS!"]);
  });

  test("a usage error goes to stderr with exit code 2", () => {
    const { io, out, err } = fakeIo();
    assert.equal(main(["--loud", "Ada"], io), 2);
    assert.deepEqual(out, []);
    assert.equal(err.length, 2);
    assert.match(err[0] ?? "", /^error: /);
    assert.equal(err[1], USAGE);
  });

  test("--help prints the usage to stdout with exit code 0", () => {
    const { io, out, err } = fakeIo();
    assert.equal(main(["--help"], io), 0);
    assert.deepEqual(out, [USAGE]);
    assert.deepEqual(err, []);
  });
});

describe("bin.ts", () => {
  const bin = fileURLToPath(new URL("../bin.ts", import.meta.url));
  const run = (args: string[], env: Record<string, string> = {}) =>
    spawnSync(process.execPath, [bin, ...args], {
      encoding: "utf8",
      env: { ...process.env, HELLO_LANG: "", ...env },
    });

  test("the real process exits with 0 and prints to stdout", () => {
    const result = run(["--lang", "es", "Ada"]);
    assert.equal(result.status, 0, result.stderr);
    assert.equal(result.stdout, "Hola, Ada!\n");
  });

  test("the real process exits with 2 on a usage error", () => {
    const result = run([]);
    assert.equal(result.status, 2);
    assert.equal(result.stdout, "");
    assert.match(result.stderr, /error: missing a name/);
  });
});
