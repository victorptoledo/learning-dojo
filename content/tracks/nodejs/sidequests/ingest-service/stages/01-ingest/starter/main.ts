// Starts the service (npm start): batches go to INGEST_DIR (default ./data), port PORT (default
// 3000, 0 picks a free one). Try it:
//   printf '{"type":"click","time":"2026-03-01T12:00:00Z"}\n' | \
//     curl -i -X POST localhost:3000/events -H "content-type: application/x-ndjson" --data-binary @-
// Stage 5 asks you to change this file; until then you do not need to.
import type { AddressInfo } from "node:net";
import { createService, fileSink } from "./service.ts";

const service = createService({ sink: fileSink(process.env.INGEST_DIR ?? "data") });
service.server.listen(Number(process.env.PORT ?? 3000), "127.0.0.1", () => {
  const { port } = service.server.address() as AddressInfo;
  console.log(`listening on http://127.0.0.1:${port}`);
});
