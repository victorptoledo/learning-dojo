// The worker thread: loads the handlers module once, then runs one job per message.
import { parentPort, workerData } from "node:worker_threads";

type Handler = (payload: unknown, context: { id: string; attempt: number }) => unknown;

interface Task {
  id: string;
  type: string;
  payload: unknown;
  attempt: number;
}

const port = parentPort;
if (port === null) throw new Error("worker.ts runs as a worker thread");

const handlers = ((await import(String(workerData.handlers))) as { default: Record<string, Handler> }).default;

port.on("message", async (task: Task) => {
  try {
    const handler = handlers[task.type];
    if (handler === undefined) throw new Error(`unknown job type ${task.type}`);
    const result = await handler(task.payload, { id: task.id, attempt: task.attempt });
    port.postMessage({ id: task.id, ok: true, result });
  } catch (error) {
    port.postMessage({ id: task.id, ok: false, error: error instanceof Error ? error.message : String(error) });
  }
});
