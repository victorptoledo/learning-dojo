import assert from "node:assert/strict";
import { spawn, type ChildProcess } from "node:child_process";
import { once } from "node:events";
import { mkdtemp, readdir, readFile, rm } from "node:fs/promises";
import { request } from "node:http";
import { connect } from "node:net";
import { tmpdir } from "node:os";
import path from "node:path";
import { after, describe, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { event, events, post, postChunks, startService, until, writtenTypes } from "./ingest.ts";

const dirs: string[] = [];
const children: ChildProcess[] = [];
after(async () => {
  for (const child of children) child.kill("SIGKILL");
  for (const dir of dirs) await rm(dir, { recursive: true, force: true });
});

describe("shutdown()", () => {
  test("lets an upload in progress finish, writes the open batch, and stops listening", async () => {
    const { port, service, sink } = await startService({ batchSize: 100, flushMs: 60_000 });
    const upload = postChunks(port, [`${event("a")}\n`, `${event("b")}\n`, `${event("c")}\n`, `${event("d")}\n`], { gapMs: 100 });
    await until(() => service.stats().bufferedEvents >= 1, 2000, "the first event");
    const result = await service.shutdown({ timeoutMs: 5000 });
    assert.deepEqual(result, { drained: true, lostEvents: 0 });
    const reply = await upload;
    assert.equal(reply.status, 202);
    assert.equal(reply.json.accepted, 4);
    assert.equal(reply.headers.connection, "close");
    assert.deepEqual(writtenTypes(sink), ["a", "b", "c", "d"]);
    await assert.rejects(post(port, `${event("late")}\n`), "a new connection after shutdown is refused");
  });

  test("ready turns 503 with the reason while it drains", async () => {
    const { port, service } = await startService();
    const upload = postChunks(port, [`${event("a")}\n`, `${event("b")}\n`], { gapMs: 300 });
    // A request that started before the shutdown (its headers are not complete yet) is answered
    // during the drain.
    const socket = connect(port, "127.0.0.1");
    await once(socket, "connect");
    socket.write("GET /health/ready HTTP/1.1\r\nHost: localhost\r\n");
    await sleep(50);
    const stopped = service.shutdown({ timeoutMs: 3000 });
    await sleep(20);
    let raw = "";
    socket.on("data", (chunk: Buffer) => (raw += chunk));
    socket.write("\r\n");
    await once(socket, "close");
    assert.match(raw, /^HTTP\/1\.1 503 /);
    assert.match(raw, /"reasons":\["shutting down"\]/);
    await upload;
    assert.equal((await stopped).drained, true);
  });

  test("gives up at the deadline when the sink is stuck, and says how many events were not written", async () => {
    const { port, service, sink } = await startService({ batchSize: 100, flushMs: 60_000 });
    sink.close();
    await post(port, events(10));
    const started = Date.now();
    const result = await service.shutdown({ timeoutMs: 300 });
    const took = Date.now() - started;
    assert.deepEqual(result, { drained: false, lostEvents: 10 });
    assert.ok(took >= 280 && took < 2000, `shutdown took ${took} ms`);
  });
});

/** Starts main.ts and resolves with the process and its port once it listens. */
async function startMain(env: Record<string, string>) {
  const kataRoot = path.join(import.meta.dirname, "..");
  const child = spawn(process.execPath, [path.join(kataRoot, "main.ts")], {
    cwd: kataRoot,
    env: { ...process.env, PORT: "0", ...env },
    stdio: ["ignore", "pipe", "pipe"],
  });
  children.push(child);
  let out = "";
  let err = "";
  child.stdout?.on("data", (chunk: Buffer) => (out += chunk));
  child.stderr?.on("data", (chunk: Buffer) => (err += chunk));
  const exited = new Promise<number | null>((resolve) => child.on("exit", (code) => resolve(code)));
  await until(() => /listening on http:\/\/127\.0\.0\.1:\d+/.test(out) || child.exitCode !== null, 5000, "main.ts to listen");
  const port = Number(/listening on http:\/\/127\.0\.0\.1:(\d+)/.exec(out)?.[1]);
  assert.ok(port > 0, `main.ts did not start: ${out}${err}`);
  return { child, port, exited, output: () => out + err };
}

async function storedTypes(dir: string): Promise<string[]> {
  const files = (await readdir(dir)).filter((name) => name.endsWith(".ndjson")).sort();
  const text = (await Promise.all(files.map((file) => readFile(path.join(dir, file), "utf8")))).join("");
  return text.split("\n").filter((line) => line !== "").map((line) => JSON.parse(line).type);
}

async function within<T>(promise: Promise<T>, ms: number, what: string): Promise<T> {
  return Promise.race([promise, sleep(ms).then(() => Promise.reject(new Error(`${what} took more than ${ms} ms`)))]);
}

describe("main.ts and signals", () => {
  test("SIGTERM: everything accepted is on disk, and the process ends by itself with code 0", async () => {
    const dir = await mkdtemp(path.join(tmpdir(), "ingest-main-"));
    dirs.push(dir);
    const { child, port, exited, output } = await startMain({ INGEST_DIR: dir });
    const reply = await post(port, events(250));
    assert.equal(reply.json.accepted, 250);
    child.kill("SIGTERM");
    const code = await within(exited, 5000, "exiting after SIGTERM");
    assert.equal(code, 0, output());
    const types = await storedTypes(dir);
    assert.equal(types.length, 250);
    assert.deepEqual(types.slice(0, 3), ["e0", "e1", "e2"]);
  });

  test("SIGTERM during an upload: the upload finishes and is stored", async () => {
    const dir = await mkdtemp(path.join(tmpdir(), "ingest-main-"));
    dirs.push(dir);
    const { child, port, exited, output } = await startMain({ INGEST_DIR: dir });
    const upload = postChunks(port, [`${event("a")}\n`, `${event("b")}\n`, `${event("c")}\n`, `${event("d")}\n`, `${event("e")}\n`], { gapMs: 120 });
    await sleep(150);
    child.kill("SIGTERM");
    const reply = await upload;
    assert.equal(reply.status, 202);
    assert.equal(reply.json.accepted, 5);
    assert.equal(await within(exited, 5000, "exiting after SIGTERM"), 0, output());
    assert.deepEqual(await storedTypes(dir), ["a", "b", "c", "d", "e"]);
  });

  test("an upload that never ends: exit code 1 at SHUTDOWN_TIMEOUT_MS, or at once on a second signal", async () => {
    for (const second of [false, true]) {
      const dir = await mkdtemp(path.join(tmpdir(), "ingest-main-"));
      dirs.push(dir);
      const { child, port, exited, output } = await startMain({ INGEST_DIR: dir, SHUTDOWN_TIMEOUT_MS: second ? "60000" : "400" });
      const stuck = request({ host: "127.0.0.1", port, path: "/events", method: "POST", agent: false, headers: { "content-type": "application/x-ndjson" } });
      stuck.on("error", () => {});
      stuck.write(`${event("open")}\n`);
      await sleep(100);
      child.kill("SIGTERM");
      if (second) {
        await sleep(200);
        child.kill("SIGINT");
      }
      const code = await within(exited, 3000, second ? "exiting on the second signal" : "exiting at the deadline");
      assert.equal(code, 1, output());
      stuck.destroy();
    }
  });
});
