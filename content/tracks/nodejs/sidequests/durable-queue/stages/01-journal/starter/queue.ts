// A durable job queue: every change is a line in an append-only journal, replayed on open.
// Useful: node:fs/promises (open, readFile, rename, truncate), node:events (EventEmitter),
// structuredClone. The README of each stage lists what to export.
import { EventEmitter } from "node:events";

export type JobState = "queued" | "running" | "done" | "failed";

export interface Job {
  /** "1", "2", ...: unique and never reused, also across restarts. */
  id: string;
  type: string;
  payload: unknown;
  priority: number;
  state: JobState;
  /** How many times the job was taken. */
  attempts: number;
  createdAt: number;
  result?: unknown;
  error?: string;
}

export interface AddOptions {
  priority?: number;
}

export const JOURNAL = "journal.ndjson";

export class Queue extends EventEmitter {
  async add(type: string, payload: unknown, options: AddOptions = {}): Promise<Job> {
    throw new Error("todo: write add()");
  }

  /** The next queued job: highest priority, then oldest. It becomes running. */
  async take(): Promise<Job | undefined> {
    throw new Error("todo: write take()");
  }

  async complete(id: string, result: unknown): Promise<Job> {
    throw new Error("todo: write complete()");
  }

  async fail(id: string, error: string): Promise<Job> {
    throw new Error("todo: write fail()");
  }

  get(id: string): Job | undefined {
    throw new Error("todo: write get()");
  }

  /** Every job (or those in one state), in the order they were added. */
  list(state?: JobState): Job[] {
    throw new Error("todo: write list()");
  }

  /** Rewrites the journal with one line per job, through a temporary file and a rename. */
  async compact(): Promise<void> {
    throw new Error("todo: write compact()");
  }

  async close(): Promise<void> {
    throw new Error("todo: write close()");
  }
}

/** Opens (or creates) the queue kept in dir, replaying its journal. */
export async function openQueue(dir: string): Promise<Queue> {
  throw new Error("todo: write openQueue()");
}
