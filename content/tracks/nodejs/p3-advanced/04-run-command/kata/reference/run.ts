// Running other programs safely: no shell, a timeout that really ends the child, bounded output.
import { spawn, type ChildProcess } from "node:child_process";
import { pipeline } from "node:stream/promises";
import type { Readable } from "node:stream";

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

/** Reads a stream into a Buffer of at most `max` bytes; the rest is read and dropped. */
function collect(stream: Readable, max: number, onTruncate: () => void): () => string {
  const chunks: Buffer[] = [];
  let size = 0;
  stream.on("data", (chunk: Buffer) => {
    const room = max - size;
    if (room <= 0) {
      onTruncate();
      return;
    }
    if (chunk.length > room) {
      onTruncate();
    }
    const kept = chunk.subarray(0, room);
    chunks.push(kept);
    size += kept.length;
  });
  return () => Buffer.concat(chunks).toString("utf8");
}

/**
 * Runs the command with these arguments, without a shell. Resolves when the child has exited
 * and its output is read, whatever the exit code; rejects only when it cannot start (for
 * example ENOENT).
 */
export function run(command: string, args: readonly string[] = [], options: RunOptions = {}): Promise<RunResult> {
  const { cwd, env = process.env, input, timeoutMs, killGraceMs = 500, maxOutputBytes = 1024 * 1024 } = options;
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { cwd, env, stdio: ["pipe", "pipe", "pipe"] });
    let truncated = false;
    let timedOut = false;
    const markTruncated = () => {
      truncated = true;
    };
    const stdout = collect(child.stdout, maxOutputBytes, markTruncated);
    const stderr = collect(child.stderr, maxOutputBytes, markTruncated);

    let killTimer: NodeJS.Timeout | undefined;
    const timer =
      timeoutMs === undefined
        ? undefined
        : setTimeout(() => {
            timedOut = true;
            child.kill("SIGTERM");
            killTimer = setTimeout(() => child.kill("SIGKILL"), killGraceMs);
          }, timeoutMs);
    const clearTimers = () => {
      clearTimeout(timer);
      clearTimeout(killTimer);
    };

    child.stdin.on("error", () => {}); // a child that exits without reading stdin gives EPIPE
    child.stdin.end(input);

    child.once("error", (error) => {
      clearTimers();
      reject(error);
    });
    child.once("close", (code, signal) => {
      clearTimers();
      resolve({ code, signal, stdout: stdout(), stderr: stderr(), timedOut, truncated });
    });
  });
}

/**
 * Like run, but resolves with stdout only when the command exited with 0, and otherwise
 * rejects with a CommandError: "<command> timed out after <ms> ms", "<command> was killed by
 * <signal>" or "<command> exited with code <code>: <first line of stderr>".
 */
export async function runOrThrow(command: string, args: readonly string[] = [], options: RunOptions = {}): Promise<string> {
  const result = await run(command, args, options);
  if (result.timedOut) {
    throw new CommandError(`${command} timed out after ${options.timeoutMs} ms`, result);
  }
  if (result.signal !== null) {
    throw new CommandError(`${command} was killed by ${result.signal}`, result);
  }
  if (result.code !== 0) {
    const firstLine = result.stderr.split("\n")[0]?.trim() ?? "";
    throw new CommandError(`${command} exited with code ${result.code}: ${firstLine}`, result);
  }
  return result.stdout;
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
  const left = spawn(first.command, first.args ?? [], { stdio: ["ignore", "pipe", "inherit"] });
  const right = spawn(second.command, second.args ?? [], { stdio: ["pipe", "pipe", "inherit"] });
  const exited = (child: ChildProcess) =>
    new Promise<number | null>((resolve, reject) => {
      child.once("error", reject);
      child.once("close", (code) => resolve(code));
    });
  const leftExit = exited(left);
  const rightExit = exited(right);
  let stdout = "";
  right.stdout.setEncoding("utf8").on("data", (chunk: string) => {
    stdout += chunk;
  });
  const piping = pipeline(left.stdout, right.stdin).catch(() => {
    // EPIPE when the second command stops reading early, like `head` in a shell: not an error
  });
  const [leftCode, rightCode] = await Promise.all([leftExit, rightExit, piping]);
  return { stdout, codes: [leftCode, rightCode] };
}
