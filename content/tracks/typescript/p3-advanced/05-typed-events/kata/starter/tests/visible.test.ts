import assert from "node:assert/strict";
import { test } from "node:test";
import { TypedEmitter } from "../events.ts";

type Events = { login: { name: string }; logout: undefined };

test("emit calls the listeners with the payload", () => {
  const emitter = new TypedEmitter<Events>();
  const seen: unknown[] = [];
  emitter.on("login", (user) => seen.push(user));
  assert.equal(emitter.emit("login", { name: "Ada" }), 1);
  assert.deepEqual(seen, [{ name: "Ada" }]);
});
