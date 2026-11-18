// mirror: makes a target folder an exact copy of a source folder, copying only what changed.
import { createHash } from "node:crypto";
import { createReadStream, createWriteStream } from "node:fs";
import { chmod, mkdir, readdir, readFile, rename, rm, rmdir, stat, utimes } from "node:fs/promises";
import path from "node:path";
import { pipeline } from "node:stream/promises";
import { parseArgs } from "node:util";

export interface Io {
  stdout: (line: string) => void;
  stderr: (line: string) => void;
}

export interface FileEntry {
  /** Relative to the scanned root, with "/" separators. */
  path: string;
  size: number;
  /** The modification time in whole milliseconds. */
  mtimeMs: number;
}

export type Action =
  | { op: "copy"; path: string; size: number }
  | { op: "update"; path: string; size: number }
  | { op: "touch"; path: string; size: number }
  | { op: "delete"; path: string; size: number };

/** True when the relative path (a folder when isDir) is excluded. */
export type Ignore = (relativePath: string, isDir: boolean) => boolean;

export interface PlanOptions {
  delete?: boolean;
  exclude?: readonly string[];
  checksum?: boolean;
}

export interface ApplyResult {
  done: number;
  failed: number;
}

export const USAGE = "usage: mirror [--dry-run] [--delete] [--checksum] [--exclude PATTERN]... <source> <target>";
export const IGNORE_FILE = ".mirrorignore";

const notIgnored: Ignore = () => false;

/** Every regular file under root, sorted by path. Symbolic links are skipped. */
export async function scan(root: string, ignore: Ignore = notIgnored): Promise<FileEntry[]> {
  const found: FileEntry[] = [];
  async function visit(dir: string, prefix: string): Promise<void> {
    const entries = await readdir(dir, { withFileTypes: true });
    for (const entry of entries) {
      const relative = prefix === "" ? entry.name : `${prefix}/${entry.name}`;
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (!ignore(relative, true)) {
          await visit(full, relative);
        }
      } else if (entry.isFile() && !ignore(relative, false)) {
        const info = await stat(full);
        found.push({ path: relative, size: info.size, mtimeMs: Math.trunc(info.mtimeMs) });
      }
    }
  }
  await visit(root, "");
  return found.sort((a, b) => (a.path < b.path ? -1 : a.path > b.path ? 1 : 0));
}

function globToRegExp(glob: string): RegExp {
  let source = "";
  for (let i = 0; i < glob.length; i++) {
    const char = glob[i] ?? "";
    if (char === "*") {
      if (glob[i + 1] === "*") {
        i++;
        if (glob[i + 1] === "/") {
          i++;
          source += "(?:.*/)?";
        } else {
          source += ".*";
        }
      } else {
        source += "[^/]*";
      }
    } else if (char === "?") {
      source += "[^/]";
    } else {
      source += char.replace(/[.+^${}()|[\]\\]/g, "\\$&");
    }
  }
  return new RegExp(`^${source}$`);
}

/** gitignore-like patterns: the last pattern that matches decides. */
export function compileIgnore(patterns: readonly string[]): Ignore {
  const rules = patterns
    .map((line) => line.trim())
    .filter((line) => line !== "" && !line.startsWith("#"))
    .map((line) => {
      let pattern = line;
      const negate = pattern.startsWith("!");
      if (negate) pattern = pattern.slice(1);
      const dirOnly = pattern.endsWith("/");
      if (dirOnly) pattern = pattern.slice(0, -1);
      const anchored = pattern.includes("/");
      if (pattern.startsWith("/")) pattern = pattern.slice(1);
      return { negate, dirOnly, anchored, regex: globToRegExp(pattern) };
    });
  return (relativePath, isDir) => {
    const name = relativePath.slice(relativePath.lastIndexOf("/") + 1);
    let ignored = false;
    for (const rule of rules) {
      if (rule.dirOnly && !isDir) continue;
      if (rule.regex.test(rule.anchored ? relativePath : name)) {
        ignored = !rule.negate;
      }
    }
    return ignored;
  };
}

async function hashFile(file: string): Promise<string> {
  const hash = createHash("sha256");
  await pipeline(createReadStream(file), hash);
  return hash.digest("hex");
}

async function scanTarget(target: string, ignore: Ignore): Promise<FileEntry[]> {
  try {
    return await scan(target, ignore);
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === "ENOENT") return [];
    throw error;
  }
}

async function ignorePatterns(source: string, options: PlanOptions): Promise<string[]> {
  let fromFile: string[] = [];
  try {
    fromFile = (await readFile(path.join(source, IGNORE_FILE), "utf8")).split(/\r?\n/);
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
  }
  return [...fromFile, ...(options.exclude ?? [])];
}

/** What to do to make target match source. Deletes come last. */
export async function plan(source: string, target: string, options: PlanOptions = {}): Promise<Action[]> {
  const ignore = compileIgnore(await ignorePatterns(source, options));
  const from = await scan(source, ignore);
  const to = new Map((await scanTarget(target, ignore)).map((entry) => [entry.path, entry]));
  const actions: Action[] = [];
  for (const entry of from) {
    const other = to.get(entry.path);
    to.delete(entry.path);
    const action = { path: entry.path, size: entry.size };
    if (other === undefined) {
      actions.push({ op: "copy", ...action });
    } else if (other.size !== entry.size) {
      actions.push({ op: "update", ...action });
    } else if (options.checksum || other.mtimeMs !== entry.mtimeMs) {
      const same =
        (await hashFile(path.join(source, entry.path))) === (await hashFile(path.join(target, entry.path)));
      if (!same) {
        actions.push({ op: "update", ...action });
      } else if (other.mtimeMs !== entry.mtimeMs) {
        actions.push({ op: "touch", ...action });
      }
    }
  }
  if (options.delete) {
    for (const entry of to.values()) {
      actions.push({ op: "delete", path: entry.path, size: entry.size });
    }
  }
  return actions;
}

export function formatPlan(actions: readonly Action[]): string[] {
  if (actions.length === 0) return ["nothing to do"];
  const count = (op: Action["op"]) => actions.filter((action) => action.op === op).length;
  const bytes = actions
    .filter((action) => action.op === "copy" || action.op === "update")
    .reduce((sum, action) => sum + action.size, 0);
  return [
    ...actions.map((action) => `${action.op} ${action.path}`),
    `copy ${count("copy")}, update ${count("update")}, touch ${count("touch")}, delete ${count("delete")}, ${bytes} bytes to transfer`,
  ];
}

function reason(error: unknown): string {
  const code = (error as NodeJS.ErrnoException).code;
  if (code === "ENOENT") return "no such file or folder";
  if (code === "ENOTDIR") return "not a folder";
  if (code === "EACCES" || code === "EPERM") return "permission denied";
  return error instanceof Error ? error.message : String(error);
}

const PAST: Record<Action["op"], string> = { copy: "copied", update: "updated", touch: "touched", delete: "deleted" };

async function copyFile(from: string, to: string): Promise<void> {
  await mkdir(path.dirname(to), { recursive: true });
  const temp = path.join(path.dirname(to), `.${path.basename(to)}.mirror-tmp`);
  try {
    const info = await stat(from);
    await pipeline(createReadStream(from), createWriteStream(temp));
    await chmod(temp, info.mode & 0o777);
    await utimes(temp, info.atime, info.mtime);
    await rename(temp, to);
  } catch (error) {
    await rm(temp, { force: true });
    throw error;
  }
}

/** Removes the folders that held deleted files once they are empty, unless source has them. */
async function pruneFolders(source: string, target: string, deleted: readonly string[]): Promise<void> {
  const folders = new Set<string>();
  for (const file of deleted) {
    for (let dir = path.posix.dirname(file); dir !== "."; dir = path.posix.dirname(dir)) {
      folders.add(dir);
    }
  }
  const deepestFirst = [...folders].sort((a, b) => b.split("/").length - a.split("/").length);
  for (const dir of deepestFirst) {
    const inSource = await stat(path.join(source, dir)).then((info) => info.isDirectory(), () => false);
    if (inSource) continue;
    try {
      await rmdir(path.join(target, dir));
    } catch {
      // not empty: something else (an excluded file, for instance) still lives there
    }
  }
}

/** Runs the actions, one line per action; a failed action does not stop the others. */
export async function apply(source: string, target: string, actions: readonly Action[], io: Io): Promise<ApplyResult> {
  const result: ApplyResult = { done: 0, failed: 0 };
  const deleted: string[] = [];
  await mkdir(target, { recursive: true });
  for (const action of actions) {
    const from = path.join(source, action.path);
    const to = path.join(target, action.path);
    try {
      if (action.op === "copy" || action.op === "update") {
        await copyFile(from, to);
      } else if (action.op === "touch") {
        const info = await stat(from);
        await utimes(to, info.atime, info.mtime);
      } else {
        await rm(to);
        deleted.push(action.path);
      }
      io.stdout(`${PAST[action.op]} ${action.path}`);
      result.done++;
    } catch (error) {
      io.stderr(`error: ${action.op} ${action.path}: ${reason(error)}`);
      result.failed++;
    }
  }
  await pruneFolders(source, target, deleted);
  return result;
}

function usageError(io: Io, message: string): number {
  io.stderr(`error: ${message}`);
  io.stderr(USAGE);
  return 2;
}

async function readableFolder(folder: string, optional: boolean): Promise<string | undefined> {
  try {
    const info = await stat(folder);
    if (!info.isDirectory()) return "not a folder";
    await readdir(folder);
    return undefined;
  } catch (error) {
    const code = (error as NodeJS.ErrnoException).code;
    if (optional && code === "ENOENT") return undefined;
    return code === "ENOENT" ? "no such folder" : reason(error);
  }
}

export async function main(argv: readonly string[], io: Io): Promise<number> {
  let parsed;
  try {
    parsed = parseArgs({
      args: [...argv],
      allowPositionals: true,
      strict: true,
      options: {
        "dry-run": { type: "boolean", default: false },
        delete: { type: "boolean", default: false },
        checksum: { type: "boolean", default: false },
        exclude: { type: "string", multiple: true, default: [] },
      },
    });
  } catch (error) {
    return usageError(io, (error as Error).message);
  }
  const { values, positionals } = parsed;
  if (positionals.length !== 2) {
    return usageError(io, "expected a source and a target folder");
  }
  const [source = "", target = ""] = positionals;
  for (const [folder, optional] of [[source, false], [target, true]] as const) {
    const problem = await readableFolder(folder, optional);
    if (problem !== undefined) {
      io.stderr(`error: cannot read ${folder}: ${problem}`);
      return 2;
    }
  }
  const from = path.resolve(source);
  const to = path.resolve(target);
  const inside = (outer: string, inner: string) => {
    const relative = path.relative(outer, inner);
    return !relative.startsWith("..") && !path.isAbsolute(relative);
  };
  if (inside(from, to) || inside(to, from)) {
    io.stderr("error: source and target overlap");
    return 2;
  }
  const actions = await plan(source, target, {
    delete: values.delete,
    checksum: values.checksum,
    exclude: values.exclude,
  });
  if (values["dry-run"]) {
    for (const line of formatPlan(actions)) io.stdout(line);
    return 0;
  }
  if (actions.length === 0) {
    io.stdout("nothing to do");
    return 0;
  }
  const result = await apply(source, target, actions, io);
  io.stdout(`done: ${result.done} ok, ${result.failed} failed`);
  return result.failed === 0 ? 0 : 1;
}
