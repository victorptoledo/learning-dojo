import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { mkdir, mkdtemp, rm, symlink, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { after, before, describe, test } from "node:test";
import { findDuplicates, formatReport, hashFile, main, USAGE, walk, type DuplicateGroup, type Io } from "../dupes.ts";

let root = "";

/** Creates the files under a fresh temporary folder: { "a/b.txt": "content" }. */
async function tree(files: Record<string, string>): Promise<string> {
  const dir = await mkdtemp(path.join(tmpdir(), "dupes-"));
  for (const [name, content] of Object.entries(files)) {
    const full = path.join(dir, ...name.split("/"));
    await mkdir(path.dirname(full), { recursive: true });
    await writeFile(full, content);
  }
  return dir;
}

async function collect(iterable: AsyncIterable<string>): Promise<string[]> {
  const out: string[] = [];
  for await (const item of iterable) {
    out.push(item);
  }
  return out;
}

function fakeIo() {
  const out: string[] = [];
  const err: string[] = [];
  const io: Io = { stdout: (line) => out.push(line), stderr: (line) => err.push(line) };
  return { io, out, err };
}

const sha = (text: string) => createHash("sha256").update(text).digest("hex");

before(async () => {
  root = await tree({
    "b.txt": "hello world",
    "a.txt": "hello world",
    "docs/readme.md": "hello world",
    "docs/notes.md": "other text!", // same size as "hello world", other content
    "src/big1.bin": "x".repeat(100),
    "src/deep/big2.bin": "x".repeat(100),
    "unique.txt": "only once",
    "empty1.txt": "",
    "empty2.txt": "",
    "node_modules/pkg/a.txt": "hello world",
    ".git/objects/a.txt": "hello world",
  });
});

after(async () => {
  await rm(root, { recursive: true, force: true });
});

describe("walk", () => {
  test("yields relative paths with / in sorted, depth-first order and skips ignored folders", async () => {
    assert.deepEqual(await collect(walk(root)), [
      "a.txt",
      "b.txt",
      "docs/notes.md",
      "docs/readme.md",
      "empty1.txt",
      "empty2.txt",
      "src/big1.bin",
      "src/deep/big2.bin",
      "unique.txt",
    ]);
  });

  test("takes its own ignore list", async () => {
    const files = await collect(walk(root, { ignore: ["docs", "src"] }));
    assert.ok(files.includes("node_modules/pkg/a.txt"));
    assert.ok(files.includes(".git/objects/a.txt"));
    assert.ok(!files.some((file) => file.startsWith("docs/") || file.startsWith("src/")));
  });

  test("skips symbolic links", async (t) => {
    const dir = await tree({ "real.txt": "content" });
    try {
      await symlink(path.join(dir, "real.txt"), path.join(dir, "link.txt"));
    } catch {
      t.skip("this system does not allow symbolic links");
      return;
    }
    assert.deepEqual(await collect(walk(dir)), ["real.txt"]);
    await rm(dir, { recursive: true, force: true });
  });

  test("rejects when the root does not exist", async () => {
    await assert.rejects(collect(walk(path.join(root, "missing"))), { code: "ENOENT" });
  });
});

describe("hashFile", () => {
  test("returns the SHA-256 in hex", async () => {
    assert.equal(await hashFile(path.join(root, "a.txt")), sha("hello world"));
  });
});

describe("findDuplicates", () => {
  test("groups equal content, largest first, ignoring same-size different files and empty files", async () => {
    assert.deepEqual(await findDuplicates(root), [
      { size: 100, hash: sha("x".repeat(100)), files: ["src/big1.bin", "src/deep/big2.bin"] },
      { size: 11, hash: sha("hello world"), files: ["a.txt", "b.txt", "docs/readme.md"] },
    ]);
  });

  test("minSize 0 includes empty files, a larger one drops small groups", async () => {
    const withEmpty = await findDuplicates(root, { minSize: 0 });
    assert.deepEqual(withEmpty.at(-1)?.files, ["empty1.txt", "empty2.txt"]);
    const bigOnly = await findDuplicates(root, { minSize: 50 });
    assert.deepEqual(
      bigOnly.map((group) => group.size),
      [100],
    );
  });

  test("returns no groups for a folder without duplicates", async () => {
    const dir = await tree({ "one.txt": "1", "two.txt": "22" });
    assert.deepEqual(await findDuplicates(dir), []);
    await rm(dir, { recursive: true, force: true });
  });
});

describe("formatReport", () => {
  test("prints each group and the wasted bytes", () => {
    const groups: DuplicateGroup[] = [
      { size: 100, hash: "h1", files: ["a.bin", "b.bin"] },
      { size: 11, hash: "h2", files: ["a.txt", "b.txt", "c.txt"] },
    ];
    assert.deepEqual(formatReport(groups), [
      "2 files, 100 bytes each:",
      "  a.bin",
      "  b.bin",
      "3 files, 11 bytes each:",
      "  a.txt",
      "  b.txt",
      "  c.txt",
      "3 duplicate files waste 122 bytes",
    ]);
  });

  test("says when there is nothing", () => {
    assert.deepEqual(formatReport([]), ["no duplicates"]);
  });
});

describe("main", () => {
  test("exit code 1 and the report when duplicates are found", async () => {
    const { io, out, err } = fakeIo();
    assert.equal(await main([root], io), 1);
    assert.deepEqual(err, []);
    assert.equal(out[0], "2 files, 100 bytes each:");
    assert.equal(out.at(-1), "3 duplicate files waste 122 bytes");
  });

  test("exit code 0 when there are none", async () => {
    const dir = await tree({ "one.txt": "1" });
    const { io, out } = fakeIo();
    assert.equal(await main([dir], io), 0);
    assert.deepEqual(out, ["no duplicates"]);
    await rm(dir, { recursive: true, force: true });
  });

  test("--json prints the groups as JSON and --min-size filters", async () => {
    const { io, out } = fakeIo();
    assert.equal(await main(["--json", "--min-size", "50", root], io), 1);
    assert.equal(out.length, 1);
    assert.deepEqual(JSON.parse(out[0] ?? ""), await findDuplicates(root, { minSize: 50 }));
  });

  test("usage errors exit with 2", async () => {
    for (const argv of [[], ["a", "b"], ["--fast", root], ["--min-size", "big", root]]) {
      const { io, out, err } = fakeIo();
      assert.equal(await main(argv, io), 2, `expected 2 for ${JSON.stringify(argv)}`);
      assert.deepEqual(out, []);
      assert.match(err[0] ?? "", /^error: /);
      assert.equal(err[1], USAGE);
    }
  });

  test("a missing folder exits with 2 and a clear message", async () => {
    const missing = path.join(root, "nope");
    const { io, err } = fakeIo();
    assert.equal(await main([missing], io), 2);
    assert.deepEqual(err, [`error: cannot read ${missing}: no such folder`]);
  });

  test("a file instead of a folder exits with 2", async () => {
    const file = path.join(root, "a.txt");
    const { io, err } = fakeIo();
    assert.equal(await main([file], io), 2);
    assert.deepEqual(err, [`error: cannot read ${file}: not a folder`]);
  });
});
