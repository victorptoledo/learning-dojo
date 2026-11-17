import { createActor, defineMachine, type StateOf, type Step } from "../machine.ts";
import type { Equal, Expect } from "./expect.ts";

const payment = defineMachine({
  initial: "idle",
  states: {
    idle: { on: { PAY: "paying" } },
    paying: { on: { OK: "paid", ERROR: "idle" } },
    paid: {},
  },
});
type Payment = typeof payment;

const actor = createActor(payment, {
  context: { amount: 10 },
  invoke: {
    paying: {
      run: async (context, signal) => {
        const amount: number = context.amount;
        const aborted: boolean = signal.aborted;
        return { amount, aborted };
      },
      done: "OK",
      failed: "ERROR",
    },
  },
});

// test: done and failed are events of the invoking state
// @ts-expect-error: PAY is not an event of paying
createActor(payment, { context: 0, invoke: { paying: { run: async () => 1, done: "PAY", failed: "ERROR" } } });

// test: only states can invoke
// @ts-expect-error: "charging" is not a state
createActor(payment, { context: 0, invoke: { charging: { run: async () => 1, done: "OK", failed: "ERROR" } } });

// test: run returns a promise
// @ts-expect-error: run must return a promise
createActor(payment, { context: 0, invoke: { paying: { run: () => 1, done: "OK", failed: "ERROR" } } });

// test: subscribe gets typed steps and snapshots
export const unsubscribe: () => void = actor.subscribe((step, snapshot) => {
  type StepType = Expect<Equal<typeof step, Step<Payment>>>;
  type SnapshotType = Expect<Equal<typeof snapshot, { state: StateOf<Payment>; context: { amount: number } }>>;
  const checks: [StepType, SnapshotType] = [true, true];
  void checks;
});

// test: waitFor takes a state and resolves to a snapshot
export const waiting: Promise<{ state: StateOf<Payment>; context: { amount: number } }> = actor.waitFor("paid", 100);
// @ts-expect-error: "done" is not a state
actor.waitFor("done");

// test: stop and stopped
actor.stop();
export const stopped: boolean = actor.stopped;
