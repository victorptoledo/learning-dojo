// The messages between the pool and its workers. Given: you do not need to change it.

export type Task =
  /** How many primes are smaller than `below`. */
  | { kind: "countPrimes"; below: number }
  /** Keeps the CPU busy for `ms` milliseconds, then answers `ms`. */
  | { kind: "spin"; ms: number }
  /** Answers with an error whose message is `message`. */
  | { kind: "fail"; message: string }
  /** Ends the worker thread with this exit code, without answering. */
  | { kind: "crash"; code: number };

/** Pool -> worker. */
export interface Request {
  id: number;
  task: Task;
}

/** Worker -> pool. */
export type Reply = { id: number; ok: true; value: number } | { id: number; ok: false; error: string };
