// A small state machine library: the transition table, typed with its literal names.

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
