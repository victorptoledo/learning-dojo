// Helpers for the contract tests: folders built under a temporary directory, fixed times and a
// fake Io that records the lines.
import { mkdir, mkdtemp, readdir, readFile, rm, stat, utimes, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { after } from "node:test";

const created: string[] = [];
after(async () => {
  for (const dir of created) {
    await rm(dir, { recursive: true, force: true }).catch(() => {});
  }
});

/** Fixed modification times, so source and target files can be made "the same". */
export const T1 = new Date("2026-01-01T10:00:00.000Z");
export const T2 = new Date("2026-02-01T10:00:00.000Z");

/** A new empty temporary folder, removed when the test file ends. */
export async function tempDir(): Promise<string> {
  const dir = await mkdtemp(path.join(tmpdir(), "mirror-"));
  created.push(dir);
  return dir;
}

/** Writes the files ({ "a/b.txt": "content" }) under dir, all with modification time `time`. */
export async function writeTree(dir: string, files: Record<string, string>, time: Date = T1): Promise<string> {
  for (const [name, content] of Object.entries(files)) {
    const full = path.join(dir, ...name.split("/"));
    await mkdir(path.dirname(full), { recursive: true });
    await writeFile(full, content);
    await utimes(full, time, time);
  }
  return dir;
}

/** A new temporary folder with these files. */
export async function tree(files: Record<string, string>, time: Date = T1): Promise<string> {
  return writeTree(await tempDir(), files, time);
}

/** Every file under dir as { "a/b.txt": "content" }, and every folder as { "a/": null }. */
export async function readTree(dir: string): Promise<Record<string, string | null>> {
  const out: Record<string, string | null> = {};
  async function visit(current: string, prefix: string): Promise<void> {
    const entries = await readdir(current, { withFileTypes: true });
    entries.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
    for (const entry of entries) {
      const relative = prefix + entry.name;
      if (entry.isDirectory()) {
        out[`${relative}/`] = null;
        await visit(path.join(current, entry.name), `${relative}/`);
      } else {
        out[relative] = await readFile(path.join(current, entry.name), "utf8");
      }
    }
  }
  await visit(dir, "");
  return out;
}

export async function mtimeOf(file: string): Promise<number> {
  return Math.trunc((await stat(file)).mtimeMs);
}

export function fakeIo() {
  const out: string[] = [];
  const err: string[] = [];
  return { io: { stdout: (line: string) => out.push(line), stderr: (line: string) => err.push(line) }, out, err };
}
