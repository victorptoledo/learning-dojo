// A small state machine library. The README of each stage lists what to export.

type Config = { initial: string; states: Record<string, { on?: Record<string, string> }> };

/** Returns the machine, typed with its literal state and event names (not yet: see the README). */
export function defineMachine(config: Config): Config {
  throw new Error("todo: write defineMachine()");
}
