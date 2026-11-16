// dupes: finds files with the same content under a folder.
import { createHash } from "node:crypto";
import { readdir, readFile, stat } from "node:fs/promises";
import path from "node:path";
import { parseArgs } from "node:util";

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

const DEFAULT_IGNORE = ["node_modules", ".git"];

/**
 * Yields every regular file under root as a path relative to root with "/" separators.
 * Entries of a folder are visited in sorted name order, folders depth first. Symbolic links
 * are skipped, and so are folders whose name is in `ignore`.
 */
export async function* walk(root: string, options: WalkOptions = {}): AsyncGenerator<string> {
  const ignore = new Set(options.ignore ?? DEFAULT_IGNORE);
  async function* visit(relative: string): AsyncGenerator<string> {
    const entries = await readdir(path.join(root, relative), { withFileTypes: true });
    entries.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
    for (const entry of entries) {
      const child = relative === "" ? entry.name : `${relative}/${entry.name}`;
      if (entry.isDirectory()) {
        if (!ignore.has(entry.name)) {
          yield* visit(child);
        }
      } else if (entry.isFile()) {
        yield child;
      }
    }
  }
  yield* visit("");
}

/** The SHA-256 of a file's content, in hex. */
export async function hashFile(file: string): Promise<string> {
  return createHash("sha256").update(await readFile(file)).digest("hex");
}

/**
 * Groups the files under root that have the same content. Only files whose size is shared
 * with another file are hashed. Groups are sorted by size (largest first), then by their
 * first file.
 */
export async function findDuplicates(root: string, options: FindOptions = {}): Promise<DuplicateGroup[]> {
  const minSize = options.minSize ?? 1;
  const bySize = new Map<number, string[]>();
  for await (const file of walk(root, options)) {
    const { size } = await stat(path.join(root, file));
    if (size >= minSize) {
      bySize.set(size, [...(bySize.get(size) ?? []), file]);
    }
  }

  const groups: DuplicateGroup[] = [];
  for (const [size, files] of bySize) {
    if (files.length < 2) {
      continue;
    }
    const byHash = new Map<string, string[]>();
    for (const file of files) {
      const hash = await hashFile(path.join(root, file));
      byHash.set(hash, [...(byHash.get(hash) ?? []), file]);
    }
    for (const [hash, same] of byHash) {
      if (same.length > 1) {
        groups.push({ size, hash, files: same.sort() });
      }
    }
  }
  return groups.sort((a, b) => b.size - a.size || (a.files[0] ?? "").localeCompare(b.files[0] ?? ""));
}

/** The text report: one block per group, then a total line. */
export function formatReport(groups: readonly DuplicateGroup[]): string[] {
  if (groups.length === 0) {
    return ["no duplicates"];
  }
  const lines: string[] = [];
  let extra = 0;
  let wasted = 0;
  for (const group of groups) {
    lines.push(`${group.files.length} files, ${group.size} bytes each:`);
    lines.push(...group.files.map((file) => `  ${file}`));
    extra += group.files.length - 1;
    wasted += group.size * (group.files.length - 1);
  }
  lines.push(`${extra} duplicate files waste ${wasted} bytes`);
  return lines;
}

function readArgs(argv: readonly string[]) {
  return parseArgs({
    args: [...argv],
    allowPositionals: true,
    strict: true,
    options: {
      json: { type: "boolean", default: false },
      "min-size": { type: "string" },
    },
  });
}

/**
 * Runs the program. Exit codes: 0 when there are no duplicates, 1 when there are some, 2 on a
 * usage error or when the folder cannot be read.
 */
export async function main(argv: readonly string[], io: Io): Promise<number> {
  let args: ReturnType<typeof readArgs>;
  try {
    args = readArgs(argv);
  } catch (error) {
    io.stderr(`error: ${error instanceof Error ? error.message : String(error)}`);
    io.stderr(USAGE);
    return 2;
  }
  const { values, positionals } = args;
  const minSize = values["min-size"] === undefined ? 1 : Number(values["min-size"]);
  if (positionals.length !== 1 || !Number.isInteger(minSize) || minSize < 0) {
    io.stderr(positionals.length !== 1 ? "error: expected exactly one folder" : "error: --min-size must be a whole number");
    io.stderr(USAGE);
    return 2;
  }

  const root = positionals[0] as string;
  let groups: DuplicateGroup[];
  try {
    groups = await findDuplicates(root, { minSize });
  } catch (error) {
    const code = (error as NodeJS.ErrnoException).code;
    const reason =
      code === "ENOENT" ? "no such folder" : code === "ENOTDIR" ? "not a folder" : code === "EACCES" ? "permission denied" : String(error);
    io.stderr(`error: cannot read ${root}: ${reason}`);
    return 2;
  }

  if (values.json) {
    io.stdout(JSON.stringify(groups, null, 2));
  } else {
    formatReport(groups).forEach((line) => io.stdout(line));
  }
  return groups.length === 0 ? 0 : 1;
}
