import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { z } from "zod";
import { createActor, defineMachine, restoreActor } from "../machine.ts";

const loader = defineMachine({
  initial: "idle",
  states: {
    idle: { on: { LOAD: "loading" } },
    loading: { on: { DONE: "ready", FAIL: "failed", CANCEL: "idle", RELOAD: "loading" } },
    ready: { on: { LOAD: "loading" } },
    failed: { on: { RETRY: "loading" } },
  },
});

type Data = { query: string; items: string[]; error: string };

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

/** A promise and the functions that settle it, to decide in the test when `run` finishes. */
function deferred<T>() {
  let resolve: (value: T) => void = () => {};
  let reject: (error: unknown) => void = () => {};
  const promise = new Promise<T>((ok, fail) => {
    resolve = ok;
    reject = fail;
  });
  return { promise, resolve, reject };
}

function setup() {
  const runs: { context: Data; signal: AbortSignal; work: ReturnType<typeof deferred<unknown>> }[] = [];
  const actor = createActor(loader, {
    context: { query: "books", items: [], error: "" } as Data,
    events: { DONE: z.object({ items: z.array(z.string()) }), FAIL: z.string() },
    updates: {
      "loading.DONE": (context, payload) => ({ ...context, items: payload.items, error: "" }),
      "loading.FAIL": (context, payload) => ({ ...context, error: payload }),
    },
    invoke: {
      loading: {
        run: (context, signal) => {
          const work = deferred<unknown>();
          runs.push({ context, signal, work });
          return work.promise;
        },
        done: "DONE",
        failed: "FAIL",
      },
    },
  });
  return { actor, runs };
}

describe("invoke", () => {
  test("runs on entering the state, with the context and a live signal", () => {
    const { actor, runs } = setup();
    assert.equal(runs.length, 0);
    actor.send("LOAD");
    assert.equal(runs.length, 1);
    assert.equal(runs[0]?.context.query, "books");
    assert.equal(runs[0]?.signal.aborted, false);
  });

  test("a resolved run sends done with the value", async () => {
    const { actor, runs } = setup();
    actor.send("LOAD");
    runs[0]?.work.resolve({ items: ["a", "b"] });
    const snapshot = await actor.waitFor("ready", 1000);
    assert.deepEqual(snapshot, { state: "ready", context: { query: "books", items: ["a", "b"], error: "" } });
    assert.deepEqual(actor.history.at(-1), { from: "loading", event: "DONE", to: "ready" });
  });

  test("a rejected run sends failed with the message", async () => {
    const { actor, runs } = setup();
    actor.send("LOAD");
    runs[0]?.work.reject(new Error("offline"));
    await actor.waitFor("failed", 1000);
    assert.equal(actor.context.error, "offline");
    actor.send("RETRY");
    runs[1]?.work.reject("plain text");
    await actor.waitFor("failed", 1000);
    assert.equal(actor.context.error, "plain text");
  });

  test("leaving the state aborts the run and ignores its result", async () => {
    const { actor, runs } = setup();
    actor.send("LOAD");
    actor.send("CANCEL");
    assert.equal(runs[0]?.signal.aborted, true);
    runs[0]?.work.resolve({ items: ["late"] });
    await sleep(20);
    assert.equal(actor.state, "idle");
    assert.deepEqual(actor.context.items, []);
    assert.deepEqual(actor.history.map((step) => step.event), ["LOAD", "CANCEL"]);
  });

  test("entering the same state again starts a new run", async () => {
    const { actor, runs } = setup();
    actor.send("LOAD");
    actor.send("RELOAD");
    assert.equal(runs.length, 2);
    assert.equal(runs[0]?.signal.aborted, true);
    assert.equal(runs[1]?.signal.aborted, false);
    runs[0]?.work.resolve({ items: ["old"] });
    runs[1]?.work.resolve({ items: ["new"] });
    const snapshot = await actor.waitFor("ready", 1000);
    assert.deepEqual(snapshot.context.items, ["new"]);
  });

  test("a value the done schema rejects leaves the actor where it is", async () => {
    const { actor, runs } = setup();
    actor.send("LOAD");
    runs[0]?.work.resolve({ items: [1, 2] });
    await sleep(20);
    assert.equal(actor.state, "loading");
  });

  test("the initial state's run starts at creation", async () => {
    const machine = defineMachine({ initial: "booting", states: { booting: { on: { UP: "up", DOWN: "down" } }, up: {}, down: {} } });
    const actor = createActor(machine, {
      context: null,
      invoke: { booting: { run: async () => "ok", done: "UP", failed: "DOWN" } },
    });
    assert.deepEqual(await actor.waitFor("up", 1000), { state: "up", context: null });
  });

  test("a restored actor runs the work of its state", async () => {
    const restored = restoreActor(loader, {
      contextSchema: z.object({ query: z.string(), items: z.array(z.string()), error: z.string() }),
      events: { DONE: z.object({ items: z.array(z.string()) }) },
      updates: { "loading.DONE": (context, payload) => ({ ...context, items: payload.items }) },
      invoke: { loading: { run: async (context) => ({ items: [context.query] }), done: "DONE", failed: "FAIL" } },
    }, { state: "loading", context: { query: "q", items: [], error: "" } });
    assert.ok(restored.ok);
    if (restored.ok) {
      const snapshot = await restored.actor.waitFor("ready", 1000);
      assert.deepEqual(snapshot.context.items, ["q"]);
    }
  });
});

describe("subscribe", () => {
  test("calls the listener after every transition with the step and the snapshot", async () => {
    const { actor, runs } = setup();
    const calls: unknown[] = [];
    actor.subscribe((step, snapshot) => calls.push([step, snapshot]));
    actor.send("LOAD");
    actor.send("CANCEL");
    actor.send("SHIP" as "LOAD");
    assert.deepEqual(calls, [
      [{ from: "idle", event: "LOAD", to: "loading" }, { state: "loading", context: { query: "books", items: [], error: "" } }],
      [{ from: "loading", event: "CANCEL", to: "idle" }, { state: "idle", context: { query: "books", items: [], error: "" } }],
    ]);
    assert.equal(runs.length, 1);
  });

  test("the function it returns unsubscribes", () => {
    const { actor } = setup();
    let calls = 0;
    const unsubscribe = actor.subscribe(() => {
      calls += 1;
    });
    actor.send("LOAD");
    unsubscribe();
    actor.send("CANCEL");
    assert.equal(calls, 1);
  });
});

describe("waitFor", () => {
  test("resolves at once when the actor is already there", async () => {
    const { actor } = setup();
    assert.deepEqual(await actor.waitFor("idle"), { state: "idle", context: { query: "books", items: [], error: "" } });
  });

  test("rejects after timeoutMs", async () => {
    const { actor } = setup();
    actor.send("LOAD");
    await assert.rejects(actor.waitFor("ready", 50), { message: "timed out waiting for ready" });
  });

  test("several waiters for the same state all resolve", async () => {
    const { actor, runs } = setup();
    actor.send("LOAD");
    const first = actor.waitFor("ready");
    const second = actor.waitFor("ready", 1000);
    runs[0]?.work.resolve({ items: ["x"] });
    const [a, b] = await Promise.all([first, second]);
    assert.equal(a.state, "ready");
    assert.equal(b.state, "ready");
  });
});

describe("stop", () => {
  test("aborts the run, rejects the waiters and freezes the actor", async () => {
    const { actor, runs } = setup();
    let calls = 0;
    actor.subscribe(() => {
      calls += 1;
    });
    actor.send("LOAD");
    const waiting = actor.waitFor("ready");
    assert.equal(actor.stopped, false);
    actor.stop();
    assert.equal(actor.stopped, true);
    assert.equal(runs[0]?.signal.aborted, true);
    await assert.rejects(waiting, { message: "actor stopped" });
    runs[0]?.work.resolve({ items: ["late"] });
    await sleep(20);
    assert.equal(actor.state, "loading");
    assert.deepEqual(actor.send("CANCEL"), { ok: false, reason: "not-allowed" });
    assert.deepEqual(actor.receive({ type: "CANCEL" }), { ok: false, reason: "not-allowed" });
    assert.deepEqual(actor.receive({ type: "NOPE" }), { ok: false, reason: "unknown-event" });
    await assert.rejects(actor.waitFor("loading"), { message: "actor stopped" });
    assert.equal(calls, 1);
  });

  test("stopping twice is harmless", () => {
    const { actor } = setup();
    actor.stop();
    actor.stop();
    assert.equal(actor.stopped, true);
  });
});
