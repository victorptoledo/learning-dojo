import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { TypedEmitter } from "../events.ts";

type Events = { login: { name: string }; logout: undefined; tick: number };

describe("TypedEmitter", () => {
  test("calls listeners in the order they were added", () => {
    const emitter = new TypedEmitter<Events>();
    const order: string[] = [];
    emitter.on("tick", () => order.push("a"));
    emitter.on("tick", () => order.push("b"));
    emitter.on("logout", () => order.push("other event"));
    assert.equal(emitter.emit("tick", 1), 2);
    assert.deepEqual(order, ["a", "b"]);
  });

  test("emit without listeners returns 0", () => {
    assert.equal(new TypedEmitter<Events>().emit("tick", 1), 0);
  });

  test("the function returned by on removes the listener", () => {
    const emitter = new TypedEmitter<Events>();
    const seen: unknown[] = [];
    const stop = emitter.on("tick", (n) => seen.push(n));
    emitter.emit("tick", 1);
    stop();
    stop();
    emitter.emit("tick", 2);
    assert.deepEqual(seen, [1]);
    assert.equal(emitter.listenerCount("tick"), 0);
  });

  test("the same listener added twice is called twice, and off removes one", () => {
    const emitter = new TypedEmitter<Events>();
    let calls = 0;
    const listener = () => {
      calls += 1;
    };
    emitter.on("tick", listener);
    emitter.on("tick", listener);
    emitter.emit("tick", 1);
    emitter.off("tick", listener);
    emitter.emit("tick", 2);
    assert.equal(calls, 3);
    assert.equal(emitter.listenerCount("tick"), 1);
  });

  test("once listens only one time", () => {
    const emitter = new TypedEmitter<Events>();
    const seen: unknown[] = [];
    emitter.once("tick", (n) => seen.push(n));
    assert.equal(emitter.listenerCount("tick"), 1);
    emitter.emit("tick", 1);
    emitter.emit("tick", 2);
    assert.deepEqual(seen, [1]);
    assert.equal(emitter.listenerCount("tick"), 0);
  });

  test("once can be cancelled before it fires", () => {
    const emitter = new TypedEmitter<Events>();
    let called = false;
    const stop = emitter.once("tick", () => {
      called = true;
    });
    stop();
    emitter.emit("tick", 1);
    assert.equal(called, false);
  });

  test("a listener added during emit waits for the next emit", () => {
    const emitter = new TypedEmitter<Events>();
    const seen: string[] = [];
    emitter.on("tick", () => {
      seen.push("first");
      emitter.on("tick", () => seen.push("late"));
    });
    emitter.emit("tick", 1);
    assert.deepEqual(seen, ["first"]);
  });

  test("a throwing listener does not stop the others", () => {
    const emitter = new TypedEmitter<Events>();
    const seen: string[] = [];
    emitter.on("tick", () => {
      throw new Error("one");
    });
    emitter.on("tick", () => seen.push("still called"));
    emitter.on("tick", () => {
      throw new Error("two");
    });
    assert.throws(
      () => emitter.emit("tick", 1),
      (error: unknown) => {
        assert.ok(error instanceof AggregateError);
        assert.deepEqual(
          error.errors.map((each: Error) => each.message),
          ["one", "two"],
        );
        return true;
      },
    );
    assert.deepEqual(seen, ["still called"]);
  });

  test("onAll maps handler names to events and removes them all", () => {
    const emitter = new TypedEmitter<Events>();
    const seen: unknown[] = [];
    const stop = emitter.onAll({
      onLogin: (user) => seen.push(user),
      onTick: (n) => seen.push(n),
    });
    emitter.emit("login", { name: "Ada" });
    emitter.emit("tick", 3);
    stop();
    emitter.emit("tick", 4);
    assert.deepEqual(seen, [{ name: "Ada" }, 3]);
    assert.equal(emitter.listenerCount("login"), 0);
  });
});

describe("waitFor", () => {
  test("resolves with the next payload and stops listening", async () => {
    const emitter = new TypedEmitter<Events>();
    const next = emitter.waitFor("login");
    assert.equal(emitter.listenerCount("login"), 1);
    emitter.emit("login", { name: "Grace" });
    assert.deepEqual(await next, { name: "Grace" });
    assert.equal(emitter.listenerCount("login"), 0);
  });

  test("rejects with the reason when aborted and stops listening", async () => {
    const emitter = new TypedEmitter<Events>();
    const controller = new AbortController();
    const next = emitter.waitFor("tick", { signal: controller.signal });
    controller.abort(new Error("gave up"));
    await assert.rejects(next, { message: "gave up" });
    assert.equal(emitter.listenerCount("tick"), 0);
  });

  test("rejects at once with an aborted signal", async () => {
    const emitter = new TypedEmitter<Events>();
    await assert.rejects(
      emitter.waitFor("tick", { signal: AbortSignal.abort(new Error("already")) }),
      { message: "already" },
    );
    assert.equal(emitter.listenerCount("tick"), 0);
  });
});
