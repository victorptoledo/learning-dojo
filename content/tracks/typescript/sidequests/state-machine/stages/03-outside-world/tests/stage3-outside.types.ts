import { z } from "zod";
import { createActor, defineMachine, restoreActor, type ReceiveResult, type SendResult, type StateOf, type Step } from "../machine.ts";
import type { Equal, Expect } from "./expect.ts";

const door = defineMachine({
  initial: "closed",
  states: { closed: { on: { OPEN: "open", LOCK: "locked" } }, open: { on: { CLOSE: "closed" } }, locked: { on: { UNLOCK: "closed" } } },
});
type Door = typeof door;
const actor = createActor(door, { context: { opened: 0 } });

// test: receive returns a ReceiveResult with the two new reasons
const received = actor.receive({ type: "OPEN" });
export type Received = Expect<Equal<typeof received, ReceiveResult<Door>>>;
export type Reasons = Expect<
  Equal<
    Extract<ReceiveResult<Door>, { ok: false }>["reason"],
    "not-allowed" | "invalid-payload" | "guard" | "invalid-event" | "unknown-event"
  >
>;
export type StillSend = Expect<Equal<Extract<SendResult<Door>, { ok: false }>["reason"], "not-allowed" | "invalid-payload" | "guard">>;

// test: history steps name states and events of the machine
export type History = Expect<Equal<(typeof actor.history)[number], Step<Door>>>;
export type StepShape = Expect<Equal<Step<Door>["event"], "OPEN" | "LOCK" | "CLOSE" | "UNLOCK">>;
// @ts-expect-error: the history is read-only
actor.history.push({ from: "open", event: "CLOSE", to: "closed" });

// test: toJSON is typed
export type Saved = Expect<Equal<ReturnType<typeof actor.toJSON>, { state: StateOf<Door>; context: { opened: number } }>>;

// test: the restored context type comes from contextSchema
const restored = restoreActor(door, { contextSchema: z.object({ opened: z.number() }) }, {});
export const opened: number | undefined = restored.ok ? restored.actor.context.opened : undefined;
export const message: string | undefined = restored.ok ? undefined : restored.error;
// @ts-expect-error: the result may be a failure
export const early = restored.actor;

// test: restoreActor takes guards and updates of the schema's context
restoreActor(door, {
  contextSchema: z.object({ opened: z.number() }),
  updates: { "closed.OPEN": (context) => ({ opened: context.opened + 1 }) },
}, {});
// @ts-expect-error: the update must return the context type
restoreActor(door, { contextSchema: z.object({ opened: z.number() }), updates: { "closed.OPEN": () => ({ opened: "x" }) } }, {});
