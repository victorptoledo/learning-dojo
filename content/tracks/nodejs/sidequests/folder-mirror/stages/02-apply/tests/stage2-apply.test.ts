import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import { chmod, mkdir, readdir, readFile, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import { describe, test } from "node:test";
import { apply, main, plan } from "../mirror.ts";
import { fakeIo, mtimeOf, readTree, T1, T2, tempDir, tree, writeTree } from "./helpers.ts";

describe("main copies", () => {
  test("creates the target and its folders, one line per action and a summary", async () => {
    const source = await tree({ "a.txt": "alpha", "docs/deep/b.md": "# b", "empty.txt": "" });
    const target = path.join(await tempDir(), "backup");
    const { io, out, err } = fakeIo();
    assert.equal(await main([source, target], io), 0);
    assert.deepEqual(err, []);
    assert.deepEqual(out, ["copied a.txt", "copied docs/deep/b.md", "copied empty.txt", "done: 3 ok, 0 failed"]);
    assert.deepEqual(await readTree(target), {
      "a.txt": "alpha",
      "docs/": null,
      "docs/deep/": null,
      "docs/deep/b.md": "# b",
      "empty.txt": "",
    });
  });

  test("keeps the modification time and the permission bits, so a second run has nothing to do", async () => {
    const source = await tree({ "run.sh": "#!/bin/sh\necho hi\n", "notes.txt": "n" });
    await chmod(path.join(source, "run.sh"), 0o750);
    await chmod(path.join(source, "notes.txt"), 0o640);
    const target = await tempDir();
    assert.equal(await main([source, target], fakeIo().io), 0);
    assert.equal(await mtimeOf(path.join(target, "run.sh")), T1.getTime());
    assert.equal((await stat(path.join(target, "run.sh"))).mode & 0o777, 0o750);
    assert.equal((await stat(path.join(target, "notes.txt"))).mode & 0o777, 0o640);
    const again = fakeIo();
    assert.equal(await main([source, target], again.io), 0);
    assert.deepEqual(again.out, ["nothing to do"]);
  });

  test("copies a large binary file byte for byte", async () => {
    const source = await tempDir();
    const content = randomBytes(6 * 1024 * 1024 + 123);
    await writeFile(path.join(source, "big.bin"), content);
    const target = await tempDir();
    assert.equal(await main([source, target], fakeIo().io), 0);
    assert.ok(content.equals(await readFile(path.join(target, "big.bin"))));
  });

  test("updates and touches existing files", async () => {
    const source = await tree({ "edited.txt": "new text", "touched.txt": "same" }, T2);
    const target = await tree({ "edited.txt": "old text", "touched.txt": "same" }, T1);
    const { io, out } = fakeIo();
    assert.equal(await main([source, target], io), 0);
    assert.deepEqual(out, ["updated edited.txt", "touched touched.txt", "done: 2 ok, 0 failed"]);
    assert.equal(await readFile(path.join(target, "edited.txt"), "utf8"), "new text");
    assert.equal(await mtimeOf(path.join(target, "touched.txt")), T2.getTime());
  });
});

describe("safe writes", () => {
  test("an update replaces the file with a new one (write to a temporary file, then rename)", async () => {
    const source = await tree({ "data.txt": "version 2, longer" });
    const target = await tree({ "data.txt": "version 1" });
    const before = (await stat(path.join(target, "data.txt"))).ino;
    assert.equal(await main([source, target], fakeIo().io), 0);
    const after = (await stat(path.join(target, "data.txt"))).ino;
    assert.notEqual(after, before, "the old file was written in place: a reader could see half a file");
    assert.equal(await readFile(path.join(target, "data.txt"), "utf8"), "version 2, longer");
  });

  test("a file that cannot be read fails alone: the old copy stays whole and no temporary file is left", async () => {
    const source = await tree({ "a.txt": "aaa", "secret.txt": "new secret, longer", "z.txt": "zzz" });
    const target = await tree({ "secret.txt": "old secret" });
    await chmod(path.join(source, "secret.txt"), 0o000);
    const { io, out, err } = fakeIo();
    assert.equal(await main([source, target], io), 1);
    assert.deepEqual(out, ["copied a.txt", "copied z.txt", "done: 2 ok, 1 failed"]);
    assert.deepEqual(err, ["error: update secret.txt: permission denied"]);
    assert.deepEqual(await readdir(target).then((names) => names.sort()), ["a.txt", "secret.txt", "z.txt"]);
    assert.equal(await readFile(path.join(target, "secret.txt"), "utf8"), "old secret");
  });

  test("a target folder that cannot be written fails its files only", async () => {
    const source = await tree({ "locked/a.txt": "a", "open/b.txt": "b" });
    const target = await tempDir();
    await mkdir(path.join(target, "locked"));
    await chmod(path.join(target, "locked"), 0o555);
    const { io, out, err } = fakeIo();
    assert.equal(await main([source, target], io), 1);
    assert.deepEqual(out, ["copied open/b.txt", "done: 1 ok, 1 failed"]);
    assert.deepEqual(err, ["error: copy locked/a.txt: permission denied"]);
    await chmod(path.join(target, "locked"), 0o755);
    assert.deepEqual(await readdir(path.join(target, "locked")), []);
  });
});

describe("--delete", () => {
  test("deletes extra files and the folders they leave empty, but not folders the source has", async () => {
    const source = await tree({ "keep/a.txt": "a" });
    await mkdir(path.join(source, "kept-empty"));
    const target = await tree({
      "keep/a.txt": "a",
      "keep/old.txt": "old",
      "kept-empty/old.txt": "old",
      "gone/deeper/old.txt": "old",
      "top.txt": "t",
    });
    const { io, out } = fakeIo();
    assert.equal(await main(["--delete", source, target], io), 0);
    assert.deepEqual(out, [
      "deleted gone/deeper/old.txt",
      "deleted keep/old.txt",
      "deleted kept-empty/old.txt",
      "deleted top.txt",
      "done: 4 ok, 0 failed",
    ]);
    assert.deepEqual(await readTree(target), { "keep/": null, "keep/a.txt": "a", "kept-empty/": null });
  });

  test("without --delete, extra files stay", async () => {
    const source = await tree({ "a.txt": "a" });
    const target = await tree({ "extra.txt": "x" });
    assert.equal(await main([source, target], fakeIo().io), 0);
    assert.deepEqual(await readTree(target), { "a.txt": "a", "extra.txt": "x" });
  });
});

describe("apply", () => {
  test("runs a plan it is given and counts what worked", async () => {
    const source = await tree({ "a.txt": "a", "b.txt": "b" });
    const target = await writeTree(await tempDir(), { "c.txt": "c" });
    const actions = await plan(source, target, { delete: true });
    const { io, out } = fakeIo();
    assert.deepEqual(await apply(source, target, actions, io), { done: 3, failed: 0 });
    assert.deepEqual(out, ["copied a.txt", "copied b.txt", "deleted c.txt"]);
  });

  test("--dry-run still changes nothing", async () => {
    const source = await tree({ "a.txt": "a" });
    const target = await tree({ "b.txt": "b" });
    assert.equal(await main(["--dry-run", "--delete", source, target], fakeIo().io), 0);
    assert.deepEqual(await readTree(target), { "b.txt": "b" });
  });
});
