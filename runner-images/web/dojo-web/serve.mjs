// `serve`: the check server, opened to the host so a learner can look at a kata in a browser.
import { startServer } from "./server.mjs";

const root = process.argv[2] ?? "/work";
const port = Number(process.argv[3] ?? 8080);
const server = await startServer({ root, host: "0.0.0.0", port });
console.log(`Serving ${root} on http://localhost:${port}/ (Ctrl+C to stop)`);
const stop = () => server.close(() => process.exit(0));
process.on("SIGINT", stop);
process.on("SIGTERM", stop);
