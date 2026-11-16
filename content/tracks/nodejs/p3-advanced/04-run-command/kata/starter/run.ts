// Running other programs safely: no shell, a timeout that really ends the child, bounded output.
// Useful imports: spawn from "node:child_process", pipeline from "node:stream/promises".

export interface RunOptions {
  cwd?: string;
  /** The whole environment of the child (default: this process's environment). */
  env?: NodeJS.ProcessEnv;
  /** Written to the child's stdin, which is then closed. Without it stdin is closed at once. */
  input?: string;
  /** After this, the child gets SIGTERM, and SIGKILL `killGraceMs` later if it is still alive. */
  timeoutMs?: number;
  /** Default 500. */
  killGraceMs?: number;
  /** stdout and stderr each keep at most this many bytes (default 1 MiB). */
  maxOutputBytes?: number;
}

export interface RunResult {
  /** The exit code, or null when the child was ended by a signal. */
  code: number | null;
  signal: NodeJS.Signals | null;
  stdout: string;
  stderr: string;
  timedOut: boolean;
  /** True when stdout or stderr was cut at maxOutputBytes. */
  truncated: boolean;
}

/** A command that failed: its result is attached. */
export class CommandError extends Error {
  override name = "CommandError";
  readonly result: RunResult;

  constructor(message: string, result: RunResult) {
    super(message);
    this.result = result;
  }
}

/**
 * Runs the command with these arguments, without a shell. Resolves when the child has exited
 * and its output is read, whatever the exit code; rejects only when it cannot start (for
 * example ENOENT).
 */
export function run(command: string, args: readonly string[] = [], options: RunOptions = {}): Promise<RunResult> {
  throw new Error("todo: write run()");
}

/**
 * Like run, but resolves with stdout only when the command exited with 0, and otherwise
 * rejects with a CommandError: "<command> timed out after <ms> ms", "<command> was killed by
 * <signal>" or "<command> exited with code <code>: <first line of stderr>".
 */
export async function runOrThrow(command: string, args: readonly string[] = [], options: RunOptions = {}): Promise<string> {
  throw new Error("todo: write runOrThrow()");
}

export interface Command {
  command: string;
  args?: readonly string[];
}

/**
 * Runs `first | second`: the stdout of the first child is streamed into the stdin of the
 * second. Resolves with the second's stdout and both exit codes; rejects when either cannot
 * start.
 */
export async function pipeCommands(first: Command, second: Command): Promise<{ stdout: string; codes: [number | null, number | null] }> {
  throw new Error("todo: write pipeCommands()");
}
