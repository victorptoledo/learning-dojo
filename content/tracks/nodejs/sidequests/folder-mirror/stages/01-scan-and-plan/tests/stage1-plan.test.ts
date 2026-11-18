import assert from "node:assert/strict";
import { chmod, mkdir, symlink, writeFile } from "node:fs/promises";
import path from "node:path";
import { describe, test } from "node:test";
import { formatPlan, main, plan, scan, USAGE, type Action } from "../mirror.ts";
import { fakeIo, T1, T2, tempDir, tree, writeTree } from "./helpers.ts";

describe("scan", () => {
  test("lists regular files with / paths, sizes and whole-millisecond times, sorted by path", async () => {
    const root = await tree({ "b.txt": "bb", "a/z.txt": "zzz", "a/b/c.txt": "c", "A.txt": "" });
    await mkdir(path.join(root, "empty"));
    assert.deepEqual(await scan(root), [
      { path: "A.txt", size: 0, mtimeMs: T1.getTime() },
      { path: "a/b/c.txt", size: 1, mtimeMs: T1.getTime() },
      { path: "a/z.txt", size: 3, mtimeMs: T1.getTime() },
      { path: "b.txt", size: 2, mtimeMs: T1.getTime() },
    ]);
  });

  test("skips symbolic links to files and to folders", async () => {
    const root = await tree({ "real/file.txt": "x" });
    await symlink(path.join(root, "real/file.txt"), path.join(root, "link.txt"));
    await symlink(path.join(root, "real"), path.join(root, "linked-dir"));
    assert.deepEqual((await scan(root)).map((entry) => entry.path), ["real/file.txt"]);
  });

  test("rejects with the system error when the root cannot be read", async () => {
    const root = await tempDir();
    await assert.rejects(scan(path.join(root, "missing")), { code: "ENOENT" });
  });
});

describe("plan", () => {
  test("copies what the target lacks and skips files with the same size and time", async () => {
    const source = await tree({ "same.txt": "same", "new.txt": "new!", "dir/deep/new.bin": "0123456789" });
    const target = await tree({ "same.txt": "same" });
    assert.deepEqual(await plan(source, target), [
      { op: "copy", path: "dir/deep/new.bin", size: 10 },
      { op: "copy", path: "new.txt", size: 4 },
    ]);
  });

  test("a missing target folder is an empty one", async () => {
    const source = await tree({ "a.txt": "a" });
    assert.deepEqual(await plan(source, path.join(await tempDir(), "not-yet")), [{ op: "copy", path: "a.txt", size: 1 }]);
  });

  test("updates on a size change, touches when only the time differs, updates when the content differs", async () => {
    const source = await tree({ "grown.txt": "longer now", "touched.txt": "same text", "edited.txt": "new text" });
    const target = await tree({ "grown.txt": "short" }, T1);
    await writeTree(target, { "touched.txt": "same text", "edited.txt": "old text" }, T2);
    assert.deepEqual(await plan(source, target), [
      { op: "update", path: "edited.txt", size: 8 },
      { op: "update", path: "grown.txt", size: 10 },
      { op: "touch", path: "touched.txt", size: 9 },
    ]);
  });

  test("does not read a file when size and time already decide", async () => {
    const source = await tree({ "same.bin": "0123", "bigger.bin": "0123456789" });
    const target = await tree({ "same.bin": "0123", "bigger.bin": "0123" });
    // Unreadable files: hashing one of them would fail with EACCES.
    for (const root of [source, target]) {
      await chmod(path.join(root, "same.bin"), 0o000);
      await chmod(path.join(root, "bigger.bin"), 0o000);
    }
    assert.deepEqual(await plan(source, target), [{ op: "update", path: "bigger.bin", size: 10 }]);
  });

  test("deletes extra target files only with delete: true, after everything else", async () => {
    const source = await tree({ "b.txt": "b" });
    const target = await tree({ "a-old.txt": "old", "z/old.txt": "older" });
    assert.deepEqual(await plan(source, target), [{ op: "copy", path: "b.txt", size: 1 }]);
    assert.deepEqual(await plan(source, target, { delete: true }), [
      { op: "copy", path: "b.txt", size: 1 },
      { op: "delete", path: "a-old.txt", size: 3 },
      { op: "delete", path: "z/old.txt", size: 5 },
    ]);
  });
});

describe("formatPlan", () => {
  test("one line per action and a summary of the bytes to transfer", () => {
    const actions: Action[] = [
      { op: "copy", path: "a.txt", size: 100 },
      { op: "update", path: "b/c.txt", size: 20 },
      { op: "touch", path: "d.txt", size: 7 },
      { op: "copy", path: "e.txt", size: 3 },
      { op: "delete", path: "old.txt", size: 50 },
    ];
    assert.deepEqual(formatPlan(actions), [
      "copy a.txt",
      "update b/c.txt",
      "touch d.txt",
      "copy e.txt",
      "delete old.txt",
      "copy 2, update 1, touch 1, delete 1, 123 bytes to transfer",
    ]);
    assert.deepEqual(formatPlan([]), ["nothing to do"]);
  });
});

describe("main --dry-run", () => {
  test("prints the plan, exits 0 and changes nothing", async () => {
    const source = await tree({ "a.txt": "a", "b.txt": "bb" });
    const target = await tree({ "b.txt": "bb", "old.txt": "x" });
    const { io, out, err } = fakeIo();
    assert.equal(await main(["--dry-run", "--delete", source, target], io), 0);
    assert.deepEqual(out, ["copy a.txt", "delete old.txt", "copy 1, update 0, touch 0, delete 1, 1 bytes to transfer"]);
    assert.deepEqual(err, []);
    assert.deepEqual((await scan(target)).map((entry) => entry.path), ["b.txt", "old.txt"]);
  });

  test("usage errors exit 2 with the usage", async () => {
    const source = await tree({ "a.txt": "a" });
    const cases = [["--dry-run", source], ["--dry-run", "--fast", source, `${source}-copy`], ["--dry-run", "a", "b", "c"]];
    for (const argv of cases) {
      const { io, out, err } = fakeIo();
      assert.equal(await main(argv, io), 2, `argv ${argv.join(" ")}`);
      assert.deepEqual(out, []);
      assert.match(err[0] ?? "", /^error: /);
      assert.equal(err[1], USAGE);
    }
  });

  test("a source that cannot be read exits 2 with the reason", async () => {
    const dir = await tempDir();
    await writeFile(path.join(dir, "file.txt"), "x");
    await mkdir(path.join(dir, "locked"));
    await chmod(path.join(dir, "locked"), 0o000);
    const cases: [string, string][] = [
      [path.join(dir, "missing"), "no such folder"],
      [path.join(dir, "file.txt"), "not a folder"],
      [path.join(dir, "locked"), "permission denied"],
    ];
    for (const [source, reason] of cases) {
      const { io, err } = fakeIo();
      assert.equal(await main(["--dry-run", source, path.join(dir, "out")], io), 2);
      assert.deepEqual(err, [`error: cannot read ${source}: ${reason}`]);
    }
    await chmod(path.join(dir, "locked"), 0o755);
  });

  test("a target that is a file exits 2", async () => {
    const source = await tree({ "a.txt": "a" });
    const dir = await tree({ "file.txt": "x" });
    const target = path.join(dir, "file.txt");
    const { io, err } = fakeIo();
    assert.equal(await main(["--dry-run", source, target], io), 2);
    assert.deepEqual(err, [`error: cannot read ${target}: not a folder`]);
  });

  test("refuses a target inside the source, a source inside the target, or the same folder", async () => {
    const source = await tree({ "a.txt": "a" });
    const cases = [
      [source, path.join(source, "backup")],
      [path.join(source, "."), source],
      [path.join(source, "sub"), source],
    ];
    for (const [from = "", to = ""] of cases) {
      await mkdir(from, { recursive: true });
      const { io, err } = fakeIo();
      assert.equal(await main(["--dry-run", from, to], io), 2, `${from} -> ${to}`);
      assert.deepEqual(err, ["error: source and target overlap"]);
    }
  });
});
