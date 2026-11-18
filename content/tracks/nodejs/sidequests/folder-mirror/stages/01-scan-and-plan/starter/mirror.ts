// mirror: makes a target folder an exact copy of a source folder, copying only what changed.
// Useful modules: node:fs/promises (readdir, stat), node:fs (createReadStream), node:crypto
// (createHash), node:stream/promises (pipeline), node:path and node:util (parseArgs).
// The README of each stage lists what to export.

export interface Io {
  stdout: (line: string) => void;
  stderr: (line: string) => void;
}

export interface FileEntry {
  /** Relative to the scanned root, with "/" separators. */
  path: string;
  size: number;
  /** The modification time in whole milliseconds (Math.trunc of stat's mtimeMs). */
  mtimeMs: number;
}

export type Action =
  | { op: "copy"; path: string; size: number }
  | { op: "update"; path: string; size: number }
  | { op: "touch"; path: string; size: number }
  | { op: "delete"; path: string; size: number };

export interface PlanOptions {
  delete?: boolean;
}

export const USAGE = "usage: mirror [--dry-run] [--delete] <source> <target>";

/** Every regular file under root, sorted by path. Symbolic links are skipped. */
export async function scan(root: string): Promise<FileEntry[]> {
  throw new Error("todo: write scan()");
}

/** What to do to make target match source. Deletes come last. */
export async function plan(source: string, target: string, options: PlanOptions = {}): Promise<Action[]> {
  throw new Error("todo: write plan()");
}

/** One line per action, then a summary line; "nothing to do" for an empty plan. */
export function formatPlan(actions: readonly Action[]): string[] {
  throw new Error("todo: write formatPlan()");
}

export async function main(argv: readonly string[], io: Io): Promise<number> {
  throw new Error("todo: write main()");
}
