// Helpers for the contract tests: temporary queue folders, removed when the test file ends.
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { after } from "node:test";
import { openQueue, type Queue } from "../queue.ts";

const dirs: string[] = [];
const queues: Queue[] = [];

after(async () => {
  for (const queue of queues) await queue.close().catch(() => {});
  for (const dir of dirs) await rm(dir, { recursive: true, force: true }).catch(() => {});
});

export async function tempDir(): Promise<string> {
  const dir = await mkdtemp(path.join(tmpdir(), "queue-"));
  dirs.push(dir);
  return dir;
}

/** Opens a queue in dir (a new temporary folder by default); closed when the file ends. */
export async function queueIn(dir?: string): Promise<{ dir: string; queue: Queue }> {
  const folder = dir ?? (await tempDir());
  const queue = await openQueue(folder);
  queues.push(queue);
  return { dir: folder, queue };
}

/** Closes the queue and opens the folder again, as a restarted process would. */
export async function reopen(dir: string, queue: Queue): Promise<Queue> {
  await queue.close();
  return (await queueIn(dir)).queue;
}
