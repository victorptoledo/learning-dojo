import assert from "node:assert/strict";
import { chmod, mkdir } from "node:fs/promises";
import path from "node:path";
import { describe, test } from "node:test";
import { compileIgnore, main, plan, scan, USAGE } from "../mirror.ts";
import { fakeIo, readTree, T1, T2, tree } from "./helpers.ts";

describe("compileIgnore", () => {
  test("a pattern without a slash matches the name at any depth", () => {
    const ignored = compileIgnore(["*.log", "node_modules", "cache?"]);
    assert.equal(ignored("app.log", false), true);
    assert.equal(ignored("logs/deep/app.log", false), true);
    assert.equal(ignored("app.log.txt", false), false);
    assert.equal(ignored("node_modules", true), true);
    assert.equal(ignored("web/node_modules", true), true);
    assert.equal(ignored("cache1", true), true);
    assert.equal(ignored("cache10", true), false);
  });

  test("a pattern with a slash matches the whole path from the root; * stops at /, ** does not", () => {
    const ignored = compileIgnore(["/dist", "build/*.map", "docs/**/draft.md"]);
    assert.equal(ignored("dist", true), true);
    assert.equal(ignored("web/dist", true), false);
    assert.equal(ignored("build/app.js.map", false), true);
    assert.equal(ignored("build/sub/app.js.map", false), false);
    assert.equal(ignored("docs/draft.md", false), true);
    assert.equal(ignored("docs/a/b/draft.md", false), true);
    assert.equal(ignored("other/draft.md", false), false);
  });

  test("a trailing slash matches folders only, # starts a comment, blank lines are skipped", () => {
    const ignored = compileIgnore(["# temporary files", "", "tmp/", "  "]);
    assert.equal(ignored("tmp", true), true);
    assert.equal(ignored("a/tmp", true), true);
    assert.equal(ignored("tmp", false), false);
    assert.equal(ignored("# temporary files", false), false);
  });

  test("! brings back what an earlier pattern excluded; the last matching pattern wins", () => {
    const ignored = compileIgnore(["*.log", "!keep.log", "logs/keep.log"]);
    assert.equal(ignored("a.log", false), true);
    assert.equal(ignored("keep.log", false), false);
    assert.equal(ignored("x/keep.log", false), false);
    assert.equal(ignored("logs/keep.log", false), true);
  });

  test("dots, brackets and other regular expression characters are literal", () => {
    const ignored = compileIgnore(["a.b", "(x)+[y]"]);
    assert.equal(ignored("a.b", false), true);
    assert.equal(ignored("axb", false), false);
    assert.equal(ignored("(x)+[y]", false), true);
    assert.equal(ignored("xxy", false), false);
  });
});

describe("excluding while mirroring", () => {
  test("scan skips what is ignored and does not enter ignored folders", async () => {
    const root = await tree({ "a.txt": "a", "app.log": "l", "secret/key.pem": "k", "src/x.ts": "x" });
    await chmod(path.join(root, "secret"), 0o000);
    const ignore = compileIgnore(["*.log", "secret/"]);
    try {
      assert.deepEqual((await scan(root, ignore)).map((entry) => entry.path), ["a.txt", "src/x.ts"]);
    } finally {
      await chmod(path.join(root, "secret"), 0o755);
    }
  });

  test("--exclude can be repeated, and excluded files are never copied", async () => {
    const source = await tree({ "a.txt": "a", "debug.log": "d", "node_modules/pkg/index.js": "js", "src/app.ts": "ts" });
    const target = path.join(await tree({}), "out");
    const { io, out } = fakeIo();
    assert.equal(await main(["--exclude", "*.log", "--exclude", "node_modules/", source, target], io), 0);
    assert.deepEqual(out, ["copied a.txt", "copied src/app.ts", "done: 2 ok, 0 failed"]);
  });

  test("excluded files in the target are protected from --delete", async () => {
    const source = await tree({ "a.txt": "a" });
    const target = await tree({ "a.txt": "a", "local.env": "SECRET=1", "cache/x.bin": "x", "old.txt": "o" });
    const { io, out } = fakeIo();
    assert.equal(await main(["--delete", "--exclude", "*.env", "--exclude", "cache/", source, target], io), 0);
    assert.deepEqual(out, ["deleted old.txt", "done: 1 ok, 0 failed"]);
    assert.deepEqual(await readTree(target), { "a.txt": "a", "cache/": null, "cache/x.bin": "x", "local.env": "SECRET=1" });
  });

  test("reads .mirrorignore from the source root before the --exclude patterns, and mirrors the file itself", async () => {
    const source = await tree({
      ".mirrorignore": "# build output\nbuild/\n*.tmp\n!important.tmp\n",
      "build/out.js": "js",
      "a.tmp": "t",
      "important.tmp": "i",
      "notes.txt": "n",
    });
    const target = await tree({});
    const actions = await plan(source, target, { exclude: ["notes.txt"] });
    assert.deepEqual(actions.map((action) => action.path), [".mirrorignore", "important.tmp"]);
    // A later --exclude wins over the ignore file's "!important.tmp".
    const again = await plan(source, target, { exclude: ["*.tmp"] });
    assert.deepEqual(again.map((action) => action.path), [".mirrorignore", "notes.txt"]);
  });

  test("a folder excluded in the target is not even read", async () => {
    const source = await tree({ "a.txt": "a" });
    const target = await tree({ "private/x.txt": "x" });
    await mkdir(path.join(target, "private", "inner"));
    await chmod(path.join(target, "private"), 0o000);
    try {
      assert.deepEqual(await plan(source, target, { delete: true, exclude: ["private/"] }), [{ op: "copy", path: "a.txt", size: 1 }]);
    } finally {
      await chmod(path.join(target, "private"), 0o755);
    }
  });
});

describe("--checksum", () => {
  test("compares the content of files with the same size even when the times match", async () => {
    const source = await tree({ "same-time.txt": "AAAA", "same.txt": "same" }, T1);
    const target = await tree({ "same-time.txt": "BBBB", "same.txt": "same" }, T1);
    assert.deepEqual(await plan(source, target), []);
    assert.deepEqual(await plan(source, target, { checksum: true }), [{ op: "update", path: "same-time.txt", size: 4 }]);
    const { io, out } = fakeIo();
    assert.equal(await main(["--checksum", source, target], io), 0);
    assert.deepEqual(out, ["updated same-time.txt", "done: 1 ok, 0 failed"]);
  });

  test("still touches files whose content is the same but whose time differs", async () => {
    const source = await tree({ "a.txt": "same" }, T2);
    const target = await tree({ "a.txt": "same" }, T1);
    assert.deepEqual(await plan(source, target, { checksum: true }), [{ op: "touch", path: "a.txt", size: 4 }]);
  });

  test("the usage lists the new options", () => {
    assert.match(USAGE, /--checksum/);
    assert.match(USAGE, /--exclude/);
  });
});
