import assert from "node:assert/strict";
import { mkdtemp, realpath, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { describe, test } from "node:test";
import { CommandError, pipeCommands, run, runOrThrow } from "../run.ts";

const node = process.execPath;
/** Runs a snippet of JavaScript in a child Node.js process. */
const js = (code: string, ...extra: string[]) => [node, ["-e", code, ...extra]] as const;

describe("run", () => {
  test("collects stdout, stderr and the exit code", async () => {
    const result = await run(...js("console.log('out'); console.error('err'); process.exitCode = 3"));
    assert.deepEqual(result, { code: 3, signal: null, stdout: "out\n", stderr: "err\n", timedOut: false, truncated: false });
  });

  test("passes arguments as they are: no shell, no injection", async () => {
    const tricky = "a b; echo $HOME && rm -rf / `whoami` 'quoted'";
    const result = await run(...js("console.log(process.argv[1])", tricky));
    assert.equal(result.stdout, `${tricky}\n`);
  });

  test("writes input to stdin and closes it", async () => {
    const result = await run(...js("let s=''; process.stdin.on('data', d => s += d).on('end', () => console.log(s.toUpperCase()))"), {
      input: "hello\nworld",
    });
    assert.equal(result.stdout, "HELLO\nWORLD\n");
  });

  test("closes stdin at once without input, so a reader does not hang", async () => {
    const result = await run(...js("process.stdin.resume().on('end', () => console.log('eof'))"), { timeoutMs: 5000 });
    assert.equal(result.stdout, "eof\n");
    assert.equal(result.timedOut, false);
  });

  test("uses cwd and env", async () => {
    const dir = await mkdtemp(path.join(tmpdir(), "run-"));
    const result = await run(...js("console.log(process.cwd()); console.log(process.env.GREETING)"), {
      cwd: dir,
      env: { ...process.env, GREETING: "hi" },
    });
    assert.deepEqual(result.stdout.trim().split(/\r?\n/), [await realpath(dir), "hi"]);
    await rm(dir, { recursive: true, force: true });
  });

  test("a timeout ends the child", async () => {
    const started = performance.now();
    const result = await run(...js("setInterval(() => {}, 1000)"), { timeoutMs: 100 });
    assert.equal(result.timedOut, true);
    assert.equal(result.code, null);
    assert.equal(result.signal, "SIGTERM");
    assert.ok(performance.now() - started < 3000);
  });

  test("a child that ignores SIGTERM gets SIGKILL after the grace period", async (t) => {
    if (process.platform === "win32") {
      t.skip("Windows has no SIGTERM handlers");
      return;
    }
    const result = await run(...js("process.on('SIGTERM', () => {}); console.log('ready'); setInterval(() => {}, 1000)"), {
      timeoutMs: 300,
      killGraceMs: 200,
    });
    assert.equal(result.timedOut, true);
    assert.equal(result.signal, "SIGKILL");
    assert.equal(result.stdout, "ready\n");
  });

  test("cuts the output at maxOutputBytes and still waits for the child", async () => {
    const result = await run(...js("process.stdout.write('x'.repeat(100000)); console.error('done')"), { maxOutputBytes: 1000 });
    assert.equal(result.stdout.length, 1000);
    assert.equal(result.truncated, true);
    assert.equal(result.stderr, "done\n");
    assert.equal(result.code, 0);
  });

  test("rejects when the command does not exist", async () => {
    await assert.rejects(run("this-command-does-not-exist-42"), { code: "ENOENT" });
  });
});

describe("runOrThrow", () => {
  test("resolves with stdout on exit code 0", async () => {
    assert.equal(await runOrThrow(...js("console.log(6 * 7)")), "42\n");
  });

  test("throws a CommandError with the first line of stderr", async () => {
    await assert.rejects(runOrThrow(...js("console.error('no such user\\nmore'); process.exit(4)")), (error: unknown) => {
      assert.ok(error instanceof CommandError);
      assert.equal(error.message, `${node} exited with code 4: no such user`);
      assert.equal(error.result.code, 4);
      return true;
    });
  });

  test("says when the command timed out", async () => {
    await assert.rejects(runOrThrow(...js("setInterval(() => {}, 1000)"), { timeoutMs: 50 }), {
      name: "CommandError",
      message: `${node} timed out after 50 ms`,
    });
  });
});

describe("pipeCommands", () => {
  test("streams the first command's stdout into the second's stdin", async () => {
    const result = await pipeCommands(
      { command: node, args: ["-e", "for (let i = 1; i <= 3; i++) console.log('line ' + i)"] },
      { command: node, args: ["-e", "let n = 0; process.stdin.on('data', d => n += String(d).split('\\n').length - 1).on('end', () => console.log(n + ' lines'))"] },
    );
    assert.deepEqual(result, { stdout: "3 lines\n", codes: [0, 0] });
  });

  test("reports both exit codes", async () => {
    const result = await pipeCommands(
      { command: node, args: ["-e", "console.log('a'); process.exitCode = 2"] },
      { command: node, args: ["-e", "process.stdin.pipe(process.stdout)"] },
    );
    assert.deepEqual(result, { stdout: "a\n", codes: [2, 0] });
  });
});
