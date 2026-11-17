// A small state machine library: the transition table, typed with its literal names, and
// actors that run it with a context, payloads checked by zod, guards and updates, events
// from outside, saved state and async effects that live as long as their state.
import { z } from "zod";

/** One state: the events it accepts and where each one goes. */
export type StateNode<Names extends string = string> = { readonly on?: { readonly [event: string]: Names } };

export type States = { readonly [state: string]: StateNode };

export type Machine<S extends States> = { readonly initial: keyof S & string; readonly states: S };

/** What every machine is assignable to (`Machine<S>` is not, because of `keyof S`). */
export type AnyMachine = { readonly initial: string; readonly states: States };

type StatesOf<M> = M extends { readonly states: infer S } ? S : never;

export type StateOf<M> = keyof StatesOf<M> & string;

type OnOf<M, S> = S extends keyof StatesOf<M> ? (StatesOf<M>[S] extends { readonly on: infer On } ? On : {}) : {};

export type EventsOf<M, S extends StateOf<M>> = keyof OnOf<M, S> & string;

export type EventOf<M> = { [S in StateOf<M>]: EventsOf<M, S> }[StateOf<M>];

export type Next<M, S extends StateOf<M>, E extends EventsOf<M, S>> = OnOf<M, S>[E & keyof OnOf<M, S>];

export function defineMachine<const S extends { readonly [state: string]: StateNode<keyof S & string> }>(config: {
  initial: NoInfer<keyof S & string>;
  states: S;
}): Machine<S> {
  return { initial: config.initial, states: config.states };
}

export class TransitionError extends Error {
  readonly state: string;
  readonly event: string;

  constructor(state: string, event: string) {
    super(`cannot ${event} in ${state}`);
    this.name = "TransitionError";
    this.state = state;
    this.event = event;
  }
}

/** The target of `event` in `state`, for names that may come from anywhere. */
function targetOf(machine: AnyMachine, state: string, event: string): string | undefined {
  if (!Object.hasOwn(machine.states, state)) {
    return undefined;
  }
  const on = machine.states[state]?.on;
  return on !== undefined && Object.hasOwn(on, event) ? on[event] : undefined;
}

export function transition<M extends AnyMachine, S extends StateOf<M>, E extends EventsOf<M, S>>(
  machine: M,
  state: S,
  event: E,
): Next<M, S, E> {
  const target = targetOf(machine, state, event);
  if (target === undefined) {
    throw new TransitionError(state, event);
  }
  return target as Next<M, S, E>;
}

export function can(machine: AnyMachine, state: string, event: string): boolean {
  return targetOf(machine, state, event) !== undefined;
}

/** The events a state accepts, in the order they are written. */
function eventsIn(machine: AnyMachine, state: string): string[] {
  return Object.keys(machine.states[state]?.on ?? {});
}

export function finalStates<M extends AnyMachine>(machine: M): StateOf<M>[] {
  return Object.keys(machine.states).filter((state) => eventsIn(machine, state).length === 0) as StateOf<M>[];
}

export function reachable<M extends AnyMachine>(machine: M): StateOf<M>[] {
  const seen: string[] = [machine.initial];
  for (let index = 0; index < seen.length; index += 1) {
    const state = seen[index] ?? "";
    for (const event of eventsIn(machine, state)) {
      const target = targetOf(machine, state, event);
      if (target !== undefined && !seen.includes(target)) {
        seen.push(target);
      }
    }
  }
  return seen as StateOf<M>[];
}

/** A zod schema per event that carries a payload. */
export type EventSchemas<M> = { readonly [E in EventOf<M>]?: z.ZodType };

/** `"open.PAY"` for every transition of the machine. */
export type TransitionKey<M> = { [S in StateOf<M>]: `${S}.${EventsOf<M, S>}` }[StateOf<M>];

type EventOfKey<K> = K extends `${string}.${infer E}` ? E : never;

type SchemaOf<Sch, E> = E extends keyof Sch ? Exclude<Sch[E], undefined> : never;

/** What a guard or an update gets: the payload as the schema parsed it. */
export type PayloadOf<Sch, E> = [SchemaOf<Sch, E>] extends [never]
  ? undefined
  : SchemaOf<Sch, E> extends z.ZodType
    ? z.output<SchemaOf<Sch, E>>
    : undefined;

type SendArgs<Sch, E> = [SchemaOf<Sch, E>] extends [never]
  ? []
  : SchemaOf<Sch, E> extends z.ZodType
    ? [payload: z.input<SchemaOf<Sch, E>>]
    : [];

type Hooks<M, C, Sch, R> = {
  readonly [K in TransitionKey<M>]?: (context: C, payload: PayloadOf<Sch, EventOfKey<K>>) => R;
};

/** The work of each state: started on entering it, aborted on leaving it. */
export type Invokes<M, C> = {
  readonly [S in StateOf<M>]?: {
    run: (context: C, signal: AbortSignal) => Promise<unknown>;
    done: EventsOf<M, S>;
    failed: EventsOf<M, S>;
  };
};

export type ActorOptions<M, C, Sch> = {
  context: C;
  events?: Sch;
  guards?: Hooks<M, C, Sch, boolean>;
  updates?: Hooks<M, C, Sch, C>;
  invoke?: Invokes<M, C>;
};

export type SendResult<M> =
  | { ok: true; state: StateOf<M> }
  | { ok: false; reason: "not-allowed" | "invalid-payload" | "guard" };

export type ReceiveResult<M> = SendResult<M> | { ok: false; reason: "invalid-event" | "unknown-event" };

export type Step<M> = { from: StateOf<M>; event: EventOf<M>; to: StateOf<M> };

export type Saved<M, C> = { state: StateOf<M>; context: C };

export type Actor<M, C, Sch> = {
  readonly state: StateOf<M>;
  readonly context: C;
  readonly history: readonly Step<M>[];
  send<E extends EventOf<M>>(event: E, ...payload: SendArgs<Sch, E>): SendResult<M>;
  receive(input: unknown): ReceiveResult<M>;
  toJSON(): Saved<M, C>;
  subscribe(listener: (step: Step<M>, snapshot: Saved<M, C>) => void): () => void;
  waitFor(state: StateOf<M>, timeoutMs?: number): Promise<Saved<M, C>>;
  stop(): void;
  readonly stopped: boolean;
};

export type RestoreOptions<M, C, Sch> = Omit<ActorOptions<M, C, Sch>, "context"> & { contextSchema: z.ZodType<C> };

export type RestoreResult<M, C, Sch> = { ok: true; actor: Actor<M, C, Sch> } | { ok: false; error: string };

type LooseHook = (context: unknown, payload: unknown) => unknown;

/** The value of an own property, for maps whose keys come from anywhere. */
function own<V>(map: { readonly [key: string]: V } | undefined, key: string): V | undefined {
  return map !== undefined && Object.hasOwn(map, key) ? map[key] : undefined;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/** Whether some state of the machine accepts `event`. */
function isEvent(machine: AnyMachine, event: string): boolean {
  return Object.keys(machine.states).some((state) => targetOf(machine, state, event) !== undefined);
}

function startActor<M extends AnyMachine, C, Sch>(
  machine: M,
  options: Omit<ActorOptions<M, C, Sch>, "context">,
  start: string,
  initialContext: C,
): Actor<M, C, Sch> {
  let state = start;
  let context = initialContext;
  const history: Step<M>[] = [];
  const schemas = options.events as { readonly [event: string]: z.ZodType | undefined } | undefined;
  const guards = options.guards as { readonly [key: string]: LooseHook | undefined } | undefined;
  const updates = options.updates as { readonly [key: string]: LooseHook | undefined } | undefined;
  const invokes = options.invoke as
    | { readonly [state: string]: { run: (context: C, signal: AbortSignal) => Promise<unknown>; done: string; failed: string } | undefined }
    | undefined;
  const listeners = new Set<(step: Step<M>, snapshot: Saved<M, C>) => void>();
  type Waiter = { state: string; resolve: (snapshot: Saved<M, C>) => void; reject: (error: Error) => void };
  const waiters = new Set<Waiter>();
  let running: AbortController | undefined;
  let stopped = false;

  const snapshot = (): Saved<M, C> => ({ state: state as StateOf<M>, context });

  /** Starts the work of the state just entered, if it has some. */
  function enter(): void {
    running?.abort();
    running = undefined;
    const work = own(invokes, state);
    if (work === undefined) {
      return;
    }
    const controller = new AbortController();
    running = controller;
    const finish = (event: string, payload: unknown) => {
      if (!controller.signal.aborted && running === controller) {
        send(event, payload);
      }
    };
    work.run(context, controller.signal).then(
      (value) => finish(work.done, value),
      (error: unknown) => finish(work.failed, error instanceof Error ? error.message : String(error)),
    );
  }

  function settle(): void {
    for (const waiter of waiters) {
      if (waiter.state === state) {
        waiters.delete(waiter);
        waiter.resolve(snapshot());
      }
    }
  }

  function send(event: string, payloadIn: unknown): SendResult<M> {
    const target = stopped ? undefined : targetOf(machine, state, event);
    if (target === undefined) {
      return { ok: false, reason: "not-allowed" };
    }
    const schema = own(schemas, event);
    let payload: unknown;
    if (schema !== undefined) {
      const parsed = schema.safeParse(payloadIn);
      if (!parsed.success) {
        return { ok: false, reason: "invalid-payload" };
      }
      payload = parsed.data;
    }
    const key = `${state}.${event}`;
    const guard = own(guards, key);
    if (guard !== undefined && guard(context, payload) !== true) {
      return { ok: false, reason: "guard" };
    }
    const update = own(updates, key);
    if (update !== undefined) {
      context = update(context, payload) as C;
    }
    const step = { from: state, event, to: target } as Step<M>;
    history.push(step);
    state = target;
    running?.abort();
    running = undefined;
    for (const listener of [...listeners]) {
      listener(step, snapshot());
    }
    settle();
    if (!stopped && state === target) {
      enter();
    }
    return { ok: true, state: target as StateOf<M> };
  }

  enter();

  return {
    get state() {
      return state as StateOf<M>;
    },
    get context() {
      return context;
    },
    get history() {
      return history;
    },
    send(event: string, ...args: unknown[]) {
      return send(event, args[0]);
    },
    receive(input: unknown): ReceiveResult<M> {
      if (!isRecord(input) || typeof input["type"] !== "string") {
        return { ok: false, reason: "invalid-event" };
      }
      if (!isEvent(machine, input["type"])) {
        return { ok: false, reason: "unknown-event" };
      }
      return send(input["type"], input["payload"]);
    },
    toJSON() {
      return snapshot();
    },
    subscribe(listener) {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
    waitFor(wanted: StateOf<M>, timeoutMs?: number): Promise<Saved<M, C>> {
      if (stopped) {
        return Promise.reject(new Error("actor stopped"));
      }
      if (state === wanted) {
        return Promise.resolve(snapshot());
      }
      return new Promise<Saved<M, C>>((resolve, reject) => {
        const timer =
          timeoutMs === undefined
            ? undefined
            : setTimeout(() => {
                waiters.delete(waiter);
                reject(new Error(`timed out waiting for ${wanted}`));
              }, timeoutMs);
        const waiter: Waiter = {
          state: wanted,
          resolve: (value) => {
            clearTimeout(timer);
            resolve(value);
          },
          reject: (error) => {
            clearTimeout(timer);
            reject(error);
          },
        };
        waiters.add(waiter);
      });
    },
    stop() {
      if (stopped) {
        return;
      }
      stopped = true;
      running?.abort();
      running = undefined;
      listeners.clear();
      for (const waiter of waiters) {
        waiter.reject(new Error("actor stopped"));
      }
      waiters.clear();
    },
    get stopped() {
      return stopped;
    },
  };
}

export function createActor<M extends AnyMachine, C, Sch extends EventSchemas<M> = {}>(
  machine: M,
  options: ActorOptions<M, C, Sch>,
): Actor<M, C, Sch> {
  return startActor(machine, options, machine.initial, options.context);
}

export function restoreActor<M extends AnyMachine, C, Sch extends EventSchemas<M> = {}>(
  machine: M,
  options: RestoreOptions<M, C, Sch>,
  saved: unknown,
): RestoreResult<M, C, Sch> {
  if (!isRecord(saved) || typeof saved["state"] !== "string") {
    return { ok: false, error: "not a saved actor" };
  }
  const state = saved["state"];
  if (!Object.hasOwn(machine.states, state)) {
    return { ok: false, error: `unknown state ${state}` };
  }
  const context = options.contextSchema.safeParse(saved["context"]);
  if (!context.success) {
    return { ok: false, error: "invalid context" };
  }
  return { ok: true, actor: startActor(machine, options, state, context.data) };
}
