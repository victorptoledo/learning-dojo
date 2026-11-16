// Starts the service on http://127.0.0.1:3000 (npm start). Stop it with Ctrl+C (SIGINT) and
// watch the shutdown in the logs. You do not need to change it.
import { setTimeout as sleep } from "node:timers/promises";
import { createLogger, createService, installShutdown } from "./service.ts";

const logger = createLogger((line) => process.stdout.write(`${line}\n`));

const service = createService(async (req, res) => {
  if (req.url === "/slow") {
    await sleep(3000); // try: curl localhost:3000/slow, then Ctrl+C while it waits
  }
  logger.info("hello from the handler");
  res.end("hello\n");
}, logger);

installShutdown(service, { logger, timeoutMs: 5000 });
service.server.listen(Number(process.env.PORT ?? 3000), "127.0.0.1", () => logger.info("listening"));
