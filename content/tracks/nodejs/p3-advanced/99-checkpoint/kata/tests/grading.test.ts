import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { mkdir, mkdtemp, realpath, rm, symlink, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { after, before, describe, test } from "node:test";
import { digestInWorker, LIMITS, ManifestError, parseManifest, resolveInside, runJobs, runTool, SecurityError } from "../runner.ts";

const isManifestError = (error: unknown) => error instanceof ManifestError;
const isSecurityError = (error: unknown) => error instanceof SecurityError;

function chained(content: string, rounds: number): string {
  let digest = createHash("sha256").update(content).digest();
  for (let round = 1; round < rounds; round += 1) {
    digest = createHash("sha256").update(digest).digest();
  }
  return digest.toString("hex");
}

let root = "";
let outside = "";

before(async () => {
  const base = await realpath(await mkdtemp(path.join(tmpdir(), "cp3-")));
  root = path.join(base, "root");
  outside = path.join(base, "outside");
  await mkdir(path.join(root, "reports", "2026"), { recursive: true });
  await mkdir(outside);
  await writeFile(path.join(root, "a.txt"), "alpha");
  await writeFile(path.join(root, "reports", "2026", "q1.csv"), "q1");
  await writeFile(path.join(outside, "secret.txt"), "top secret");
});

after(async () => {
  await rm(path.dirname(root), { recursive: true, force: true });
});

describe("parseManifest", () => {
  test("accepts a valid manifest, fills the defaults and keeps only known fields", () => {
    const manifest = parseManifest(
      JSON.stringify({
        jobs: [
          { id: "sum-1", kind: "digest", file: "a.txt", extra: true },
          { id: "hello", kind: "tool", tool: "echo", args: ["hi there"] },
          { id: "env", kind: "tool", tool: "env" },
          { id: "deep", kind: "digest", file: "a.txt", rounds: 5 },
        ],
      }),
    );
    assert.deepEqual(manifest, {
      jobs: [
        { id: "sum-1", kind: "digest", file: "a.txt", rounds: 1 },
        { id: "hello", kind: "tool", tool: "echo", args: ["hi there"] },
        { id: "env", kind: "tool", tool: "env", args: [] },
        { id: "deep", kind: "digest", file: "a.txt", rounds: 5 },
      ],
    });
  });

  test("rejects text that is not a manifest", () => {
    const job = { id: "a", kind: "tool", tool: "echo" };
    const tooMany = Array.from({ length: LIMITS.maxJobs + 1 }, (_, index) => ({ ...job, id: `job-${index}` }));
    for (const text of ["{jobs: []}", "", "[]", "null", '{"jobs": {}}', '{"jobs": []}', JSON.stringify({ jobs: tooMany })]) {
      assert.throws(() => parseManifest(text), isManifestError, text.slice(0, 40));
    }
  });

  test("rejects __proto__, constructor and prototype keys at any depth, and pollutes nothing", () => {
    const texts = [
      '{"__proto__": {"polluted": true}, "jobs": [{"id": "a", "kind": "tool", "tool": "echo"}]}',
      '{"jobs": [{"id": "a", "kind": "tool", "tool": "echo", "__proto__": {"polluted": true}}]}',
      '{"jobs": [{"id": "a", "kind": "tool", "tool": "echo", "constructor": {"prototype": {"polluted": true}}}]}',
    ];
    for (const text of texts) {
      assert.throws(() => parseManifest(text), isManifestError, text);
    }
    assert.equal(({} as Record<string, unknown>).polluted, undefined);
  });

  test("rejects bad ids, duplicate ids, bad rounds and unknown kinds", () => {
    const bad = [
      { id: "Upper", kind: "tool", tool: "echo" },
      { id: "-dash", kind: "tool", tool: "echo" },
      { id: "x".repeat(33), kind: "tool", tool: "echo" },
      { id: 7, kind: "tool", tool: "echo" },
      { id: "a", kind: "digest", file: "" },
      { id: "a", kind: "digest", file: "a.txt", rounds: 0 },
      { id: "a", kind: "digest", file: "a.txt", rounds: 1.5 },
      { id: "a", kind: "digest", file: "a.txt", rounds: "3" },
      { id: "a", kind: "digest", file: "a.txt", rounds: LIMITS.maxRounds + 1 },
      { id: "a", kind: "shell", command: "ls" },
    ];
    for (const job of bad) {
      assert.throws(() => parseManifest(JSON.stringify({ jobs: [job] })), isManifestError, JSON.stringify(job));
    }
    const twice = { id: "same", kind: "tool", tool: "echo" };
    assert.throws(() => parseManifest(JSON.stringify({ jobs: [twice, twice] })), isManifestError);
  });

  test("accepts only the tool's own names and arguments that cannot become options", () => {
    const bad = [
      { tool: "toString" },
      { tool: "hasOwnProperty" },
      { tool: "rm" },
      { tool: "echo", args: ["--eval=process.exit(9)"] },
      { tool: "echo", args: ["-p"] },
      { tool: "echo", args: [1] },
      { tool: "echo", args: "hi" },
      { tool: "echo", args: ["nul\0byte"] },
      { tool: "echo", args: Array.from({ length: LIMITS.maxArgs + 1 }, () => "x") },
    ];
    for (const fields of bad) {
      const text = JSON.stringify({ jobs: [{ id: "a", kind: "tool", ...fields }] });
      assert.throws(() => parseManifest(text), isManifestError, text);
    }
    assert.doesNotThrow(() => parseManifest(JSON.stringify({ jobs: [{ id: "a", kind: "tool", tool: "echo", args: ["a-b", "x=-1"] }] })));
  });
});

describe("resolveInside", () => {
  test("resolves files inside the root to their real path", async () => {
    assert.equal(await resolveInside(root, "a.txt"), path.join(root, "a.txt"));
    assert.equal(await resolveInside(root, "reports/2026/../2026/q1.csv"), path.join(root, "reports", "2026", "q1.csv"));
    await assert.rejects(resolveInside(root, "missing.txt"), (error: NodeJS.ErrnoException) => error.code === "ENOENT");
  });

  test("rejects paths that leave the root, before touching them", async () => {
    for (const file of ["../outside/secret.txt", "reports/../../outside/secret.txt", "..", path.join(outside, "secret.txt"), "/etc/passwd", "a.txt\0.png", "../root-sibling"]) {
      await assert.rejects(resolveInside(root, file), isSecurityError, file);
    }
  });

  test("follows symbolic links and rejects the ones that point outside", { skip: process.platform === "win32" }, async () => {
    await symlink(path.join(outside, "secret.txt"), path.join(root, "innocent.txt"));
    await symlink(path.join(root, "reports"), path.join(root, "shortcut"));
    await assert.rejects(resolveInside(root, "innocent.txt"), isSecurityError);
    assert.equal(await resolveInside(root, "shortcut/2026/q1.csv"), path.join(root, "reports", "2026", "q1.csv"));
  });
});

describe("digestInWorker", () => {
  test("computes the chained SHA-256 of the file", async () => {
    const file = path.join(root, "a.txt");
    assert.equal(await digestInWorker(file, 1), chained("alpha", 1));
    assert.equal(await digestInWorker(file, 3), chained("alpha", 3));
  });

  test("keeps the main thread free while it works", async () => {
    let last = performance.now();
    let longestGap = 0;
    const tick = () => {
      const now = performance.now();
      longestGap = Math.max(longestGap, now - last);
      last = now;
    };
    const timer = setInterval(tick, 5);
    const started = performance.now();
    let digests: string[];
    try {
      digests = await Promise.all([digestInWorker(path.join(root, "a.txt"), LIMITS.maxRounds), digestInWorker(path.join(root, "a.txt"), LIMITS.maxRounds)]);
    } finally {
      tick(); // the gap since the last tick counts too
      clearInterval(timer);
    }
    const elapsed = performance.now() - started;
    assert.equal(digests[0], chained("alpha", LIMITS.maxRounds));
    assert.equal(digests[1], digests[0]);
    assert.ok(longestGap < 100, `the event loop was blocked for ${Math.round(longestGap)} ms of ${Math.round(elapsed)} ms`);
  });

  test("rejects with the worker's error when the file cannot be read", async () => {
    await assert.rejects(digestInWorker(path.join(root, "nope.txt"), 1), /ENOENT/);
  });
});

describe("runTool", () => {
  test("passes arguments as they are and no environment but PATH", async () => {
    process.env.API_TOKEN = "s3cr3t";
    try {
      const tricky = "$(whoami); rm -rf ~ && echo `id` | cat 'quoted' \"double\"";
      assert.equal(await runTool("echo", [tricky, "two words"]), `${tricky}\ntwo words`);
      assert.equal(await runTool("env", []), "PATH");
    } finally {
      delete process.env.API_TOKEN;
    }
  });

  test("rejects with the exit code and the first line of stderr", async () => {
    await assert.rejects(runTool("fail", ["3"]), { message: "fail exited with code 3: boom 3" });
  });

  test("kills a tool that runs too long", async () => {
    const started = performance.now();
    await assert.rejects(runTool("sleep", ["10000"], { timeoutMs: 300 }), { message: "sleep timed out after 300 ms" });
    assert.ok(performance.now() - started < 3_000, "the child was not killed");
  });

  test("kills a tool that prints too much", async () => {
    assert.equal((await runTool("flood", [String(LIMITS.maxOutputBytes)])).length, LIMITS.maxOutputBytes);
    await assert.rejects(runTool("flood", [String(50 * 1024 * 1024)]), { message: `flood printed more than ${LIMITS.maxOutputBytes} bytes` });
  });
});

describe("runJobs", () => {
  test("reports every job in manifest order; a failing job does not stop the others", async () => {
    const manifest = parseManifest(
      JSON.stringify({
        jobs: [
          { id: "fail", kind: "tool", tool: "fail", args: ["2"] },
          { id: "escape", kind: "digest", file: "../outside/secret.txt" },
          { id: "sum", kind: "digest", file: "reports/2026/q1.csv", rounds: 2 },
          { id: "hi", kind: "tool", tool: "echo", args: ["hi"] },
          { id: "gone", kind: "digest", file: "gone.txt" },
        ],
      }),
    );
    const results = await runJobs(manifest, { root });
    assert.deepEqual(
      results.map((each) => [each.id, each.ok]),
      [
        ["fail", false],
        ["escape", false],
        ["sum", true],
        ["hi", true],
        ["gone", false],
      ],
    );
    assert.deepEqual(results[2], { id: "sum", ok: true, output: chained("q1", 2) });
    assert.deepEqual(results[3], { id: "hi", ok: true, output: "hi" });
    assert.ok(!results[0]!.ok && results[0]!.error === "fail exited with code 2: boom 2");
    assert.ok(!results[1]!.ok && /outside/.test(results[1]!.error), "the escape is reported as a security error");
    assert.ok(!results[4]!.ok && /ENOENT/.test(results[4]!.error));
  });

  test("runs at most `concurrency` jobs at the same time, and uses all of them", async () => {
    const jobs = Array.from({ length: 6 }, (_, index) => ({ id: `nap-${index}`, kind: "tool", tool: "sleep", args: ["400"] }));
    const results = await runJobs(parseManifest(JSON.stringify({ jobs })), { root, concurrency: 2 });
    const spans = results.map((each) => {
      assert.ok(each.ok, JSON.stringify(each));
      const [start, end] = each.output.split(" ").map(Number);
      return { start: start!, end: end! };
    });
    const overlap = (time: number) => spans.filter((span) => span.start <= time && time < span.end).length;
    const most = Math.max(...spans.map((span) => overlap(span.start)));
    assert.equal(most, 2, `at most ${most} jobs ran at the same time`);
  });
});
