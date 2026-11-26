// A batch job runner: an untrusted JSON manifest of jobs, CPU-bound digests on worker
// threads, allow-listed tools in child processes, and files confined to one folder.
import { spawn } from "node:child_process";
import { realpath } from "node:fs/promises";
import path from "node:path";
import { Worker } from "node:worker_threads";

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

const FORBIDDEN_KEYS = new Set(["__proto__", "constructor", "prototype"]);
const ID = /^[a-z0-9][a-z0-9-]{0,31}$/;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function parseJob(raw: unknown, index: number): Job {
  if (!isRecord(raw)) {
    throw new ManifestError(`job ${index} must be an object`);
  }
  const { id, kind } = raw;
  if (typeof id !== "string" || !ID.test(id)) {
    throw new ManifestError(`job ${index} needs an id like "report-1"`);
  }
  if (kind === "digest") {
    const { file, rounds = 1 } = raw;
    if (typeof file !== "string" || file.length === 0) {
      throw new ManifestError(`${id}: file must be a non-empty string`);
    }
    if (typeof rounds !== "number" || !Number.isInteger(rounds) || rounds < 1 || rounds > LIMITS.maxRounds) {
      throw new ManifestError(`${id}: rounds must be a whole number from 1 to ${LIMITS.maxRounds}`);
    }
    return { id, kind, file, rounds };
  }
  if (kind === "tool") {
    const { tool, args = [] } = raw;
    if (typeof tool !== "string" || !Object.hasOwn(TOOLS, tool)) {
      throw new ManifestError(`${id}: unknown tool`);
    }
    if (!Array.isArray(args) || args.length > LIMITS.maxArgs) {
      throw new ManifestError(`${id}: args must be a list of at most ${LIMITS.maxArgs} strings`);
    }
    const clean: string[] = [];
    for (const arg of args) {
      if (typeof arg !== "string" || arg.startsWith("-") || arg.includes("\0")) {
        throw new ManifestError(`${id}: every argument must be a string that does not start with "-"`);
      }
      clean.push(arg);
    }
    return { id, kind, tool: tool as ToolName, args: clean };
  }
  throw new ManifestError(`${id}: kind must be "digest" or "tool"`);
}

/** Parses and validates an untrusted manifest. Throws ManifestError for anything wrong. */
export function parseManifest(text: string): Manifest {
  let raw: unknown;
  try {
    raw = JSON.parse(text, (key, value: unknown) => {
      if (FORBIDDEN_KEYS.has(key)) {
        throw new ManifestError(`forbidden key: ${key}`);
      }
      return value;
    });
  } catch (error) {
    if (error instanceof ManifestError) {
      throw error;
    }
    throw new ManifestError("the manifest is not valid JSON", { cause: error });
  }
  if (!isRecord(raw) || !Array.isArray(raw.jobs)) {
    throw new ManifestError("the manifest must be an object with a jobs list");
  }
  if (raw.jobs.length === 0 || raw.jobs.length > LIMITS.maxJobs) {
    throw new ManifestError(`the manifest must have from 1 to ${LIMITS.maxJobs} jobs`);
  }
  const seen = new Set<string>();
  const jobs = raw.jobs.map((each, index) => {
    const job = parseJob(each, index);
    if (seen.has(job.id)) {
      throw new ManifestError(`duplicate id: ${job.id}`);
    }
    seen.add(job.id);
    return job;
  });
  return { jobs };
}

function inside(root: string, candidate: string): boolean {
  const relative = path.relative(root, candidate);
  return relative === "" || (!relative.startsWith("..") && !path.isAbsolute(relative));
}

/** The real path of `file` under `root`, or SecurityError when it would leave the root. */
export async function resolveInside(root: string, file: string): Promise<string> {
  if (file.includes("\0") || path.isAbsolute(file) || path.win32.isAbsolute(file)) {
    throw new SecurityError(`${file} is outside the root`);
  }
  const realRoot = await realpath(root);
  const candidate = path.resolve(realRoot, file);
  if (!inside(realRoot, candidate)) {
    throw new SecurityError(`${file} is outside the root`);
  }
  const real = await realpath(candidate); // follows symbolic links; ENOENT for a missing file
  if (!inside(realRoot, real)) {
    throw new SecurityError(`${file} is outside the root`);
  }
  return real;
}

/** The chained SHA-256 of a file, computed on a worker thread. */
export function digestInWorker(file: string, rounds: number): Promise<string> {
  return new Promise((resolve, reject) => {
    const worker = new Worker(DIGEST_WORKER, { workerData: { file, rounds } });
    let settled = false;
    worker.once("message", (hex: string) => {
      settled = true;
      resolve(hex);
    });
    worker.once("error", (error) => {
      settled = true;
      reject(error);
    });
    worker.once("exit", (code) => {
      if (!settled) {
        reject(new Error(`digest worker exited with code ${code}`));
      }
    });
  });
}

/** Runs an allow-listed tool without a shell, with only PATH in its environment. */
export function runTool(tool: ToolName, args: readonly string[], options: { timeoutMs?: number } = {}): Promise<string> {
  const timeoutMs = options.timeoutMs ?? LIMITS.toolTimeoutMs;
  const [command, ...fixed] = TOOLS[tool];
  return new Promise((resolve, reject) => {
    const child = spawn(command, [...fixed, ...args], {
      shell: false,
      env: { PATH: process.env.PATH ?? "" },
      stdio: ["ignore", "pipe", "pipe"],
    });
    const stdout: Buffer[] = [];
    let stdoutBytes = 0;
    let stderr = "";
    let failure: Error | undefined;
    const stop = (error: Error) => {
      failure ??= error;
      child.kill("SIGKILL");
    };
    const timer = setTimeout(() => stop(new Error(`${tool} timed out after ${timeoutMs} ms`)), timeoutMs);
    child.stdout.on("data", (chunk: Buffer) => {
      stdoutBytes += chunk.length;
      if (stdoutBytes > LIMITS.maxOutputBytes) {
        stop(new Error(`${tool} printed more than ${LIMITS.maxOutputBytes} bytes`));
        return;
      }
      stdout.push(chunk);
    });
    child.stderr.setEncoding("utf8");
    child.stderr.on("data", (chunk: string) => {
      if (stderr.length < 4096) {
        stderr += chunk;
      }
    });
    child.once("error", (error) => {
      clearTimeout(timer);
      reject(error);
    });
    child.once("close", (code, signal) => {
      clearTimeout(timer);
      if (failure !== undefined) {
        reject(failure);
      } else if (code !== 0) {
        const firstLine = stderr.split("\n")[0]?.trim() ?? "";
        reject(new Error(`${tool} exited with code ${code ?? signal}: ${firstLine}`));
      } else {
        resolve(Buffer.concat(stdout).toString("utf8").trimEnd());
      }
    });
  });
}

async function runJob(job: Job, root: string): Promise<JobResult> {
  try {
    if (job.kind === "digest") {
      const file = await resolveInside(root, job.file);
      return { id: job.id, ok: true, output: await digestInWorker(file, job.rounds) };
    }
    return { id: job.id, ok: true, output: await runTool(job.tool, job.args) };
  } catch (error) {
    return { id: job.id, ok: false, error: error instanceof Error ? error.message : String(error) };
  }
}

/** Runs every job, at most `concurrency` at a time, and reports them in manifest order. */
export async function runJobs(manifest: Manifest, options: { root: string; concurrency?: number }): Promise<JobResult[]> {
  const concurrency = options.concurrency ?? 2;
  if (!Number.isInteger(concurrency) || concurrency < 1) {
    throw new RangeError(`concurrency must be a whole number of at least 1, got ${concurrency}`);
  }
  const results = new Array<JobResult>(manifest.jobs.length);
  let next = 0;
  async function lane(): Promise<void> {
    while (next < manifest.jobs.length) {
      const index = next;
      next += 1;
      results[index] = await runJob(manifest.jobs[index]!, options.root);
    }
  }
  await Promise.all(Array.from({ length: Math.min(concurrency, manifest.jobs.length) }, lane));
  return results;
}
