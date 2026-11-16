// dupes: finds files with the same content under a folder.
// Useful modules: node:fs/promises (readdir, readFile, stat), node:path, node:crypto
// (createHash) and node:util (parseArgs).

export interface Io {
  stdout: (line: string) => void;
  stderr: (line: string) => void;
}

export interface WalkOptions {
  /** Folder names never entered (default: node_modules and .git). */
  ignore?: readonly string[];
}

export interface FindOptions extends WalkOptions {
  /** Files smaller than this many bytes are skipped (default 1, so empty files are skipped). */
  minSize?: number;
}

export interface DuplicateGroup {
  size: number;
  hash: string;
  /** Paths relative to the root, with "/" separators, sorted. */
  files: string[];
}

export const USAGE = "usage: dupes [--json] [--min-size BYTES] <folder>";

/**
 * Yields every regular file under root as a path relative to root with "/" separators.
 * Entries of a folder are visited in sorted name order, folders depth first. Symbolic links
 * are skipped, and so are folders whose name is in `ignore`.
 */
export async function* walk(root: string, options: WalkOptions = {}): AsyncGenerator<string> {
  throw new Error("todo: write walk()");
}

/** The SHA-256 of a file's content, in hex. */
export async function hashFile(file: string): Promise<string> {
  throw new Error("todo: write hashFile()");
}

/**
 * Groups the files under root that have the same content. Only files whose size is shared
 * with another file are hashed. Groups are sorted by size (largest first), then by their
 * first file.
 */
export async function findDuplicates(root: string, options: FindOptions = {}): Promise<DuplicateGroup[]> {
  throw new Error("todo: write findDuplicates()");
}

/** The text report: one block per group, then a total line. */
export function formatReport(groups: readonly DuplicateGroup[]): string[] {
  throw new Error("todo: write formatReport()");
}

/**
 * Runs the program. Exit codes: 0 when there are no duplicates, 1 when there are some, 2 on a
 * usage error or when the folder cannot be read.
 */
export async function main(argv: readonly string[], io: Io): Promise<number> {
  throw new Error("todo: write main()");
}
