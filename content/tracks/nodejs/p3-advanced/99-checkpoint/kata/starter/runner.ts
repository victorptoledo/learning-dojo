// A batch job runner: an untrusted JSON manifest of jobs, CPU-bound digests on worker
// threads, allow-listed tools in child processes, and files confined to one folder.
// Useful imports: spawn from "node:child_process", realpath from "node:fs/promises",
// path from "node:path", Worker from "node:worker_threads".
const node = process.execPath;

/**
 * The only programs a manifest may run: a name, then the command and its fixed arguments.
 * The job's own arguments are appended after them. Every tool is a small Node.js script, so
 * the kata works the same on any system.
 */
export const TOOLS = {
  /** Prints each argument on its own line. */
  echo: [node, "-e", "for (const arg of process.argv.slice(1)) console.log(arg)"],
  /** Prints the names of the environment variables it received, sorted. */
  env: [node, "-e", "console.log(Object.keys(process.env).sort().join(' '))"],
  /** Waits the given milliseconds, then prints the start and end times (Date.now()). */
  sleep: [node, "-e", "const start = Date.now(); setTimeout(() => console.log(start, Date.now()), Number(process.argv[1]))"],
  /** Writes a line to stderr and exits with the given code. */
  fail: [node, "-e", "console.error('boom ' + process.argv[1]); console.error('second line'); process.exit(Number(process.argv[1]))"],
  /** Writes the given number of bytes to stdout. */
  flood: [node, "-e", "process.stdout.write('x'.repeat(Number(process.argv[1])))"],
} as const satisfies Record<string, readonly [string, ...string[]]>;

export type ToolName = keyof typeof TOOLS;

export const LIMITS = {
  maxJobs: 50,
  maxRounds: 500_000,
  maxArgs: 8,
  toolTimeoutMs: 2_000,
  maxOutputBytes: 64 * 1024,
} as const;

export type Job =
  | { id: string; kind: "digest"; file: string; rounds: number }
  | { id: string; kind: "tool"; tool: ToolName; args: string[] };

export interface Manifest {
  jobs: Job[];
}

export type JobResult = { id: string; ok: true; output: string } | { id: string; ok: false; error: string };

/** The manifest is rejected as a whole: nothing runs. */
export class ManifestError extends Error {
  override name = "ManifestError";
}

/** A path that would leave the root folder. */
export class SecurityError extends Error {
  override name = "SecurityError";
}

export const DIGEST_WORKER = new URL("./digest-worker.ts", import.meta.url);

/**
 * Parses and validates an untrusted manifest (see the README for every rule). Throws
 * ManifestError for anything wrong, and returns new objects with only the known fields.
 */
export function parseManifest(text: string): Manifest {
  throw new Error("todo: write parseManifest()");
}

/** The real path of `file` under `root`, or SecurityError when it would leave the root. */
export async function resolveInside(root: string, file: string): Promise<string> {
  throw new Error("todo: write resolveInside()");
}

/** The chained SHA-256 of a file, computed on a worker thread started from DIGEST_WORKER. */
export function digestInWorker(file: string, rounds: number): Promise<string> {
  throw new Error("todo: write digestInWorker()");
}

/** Runs an allow-listed tool without a shell, with only PATH in its environment. */
export function runTool(tool: ToolName, args: readonly string[], options: { timeoutMs?: number } = {}): Promise<string> {
  throw new Error("todo: write runTool()");
}

/** Runs every job, at most `concurrency` (default 2) at a time, and reports them in manifest order. */
export async function runJobs(manifest: Manifest, options: { root: string; concurrency?: number }): Promise<JobResult[]> {
  throw new Error("todo: write runJobs()");
}
