// Starts the service (npm start): batches go to INGEST_DIR (default ./data), port PORT (default
// 3000, 0 picks a free one). SIGTERM or SIGINT shut it down gracefully within
// SHUTDOWN_TIMEOUT_MS (default 10000); a second signal exits at once.
import type { AddressInfo } from "node:net";
import { createService, fileSink } from "./service.ts";

const service = createService({ sink: fileSink(process.env.INGEST_DIR ?? "data") });
service.server.listen(Number(process.env.PORT ?? 3000), "127.0.0.1", () => {
  const { port } = service.server.address() as AddressInfo;
  console.log(`listening on http://127.0.0.1:${port}`);
});

let stopping = false;
const onSignal = (signal: NodeJS.Signals) => {
  if (stopping) {
    console.error(`${signal} again: exiting now`);
    process.exit(1);
  }
  stopping = true;
  console.log(`${signal}: shutting down`);
  void service.shutdown({ timeoutMs: Number(process.env.SHUTDOWN_TIMEOUT_MS ?? 10_000) }).then((result) => {
    console.log(JSON.stringify({ msg: "stopped", ...result }));
    // Drained: nothing is left open, so the process ends by itself with code 0. Not drained:
    // a write may still be stuck, so leave now.
    if (!result.drained) process.exit(1);
  });
};
process.on("SIGTERM", onSignal);
process.on("SIGINT", onSignal);
