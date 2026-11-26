// Starts the gateway on http://127.0.0.1:3000 (npm start). You do not need to change it.
//   UPSTREAM=http://127.0.0.1:4000 npm start
//   curl -i localhost:3000/products/pen
import { createGateway } from "./gateway.ts";

const port = Number(process.env.PORT ?? 3000);
const gateway = createGateway({ upstream: process.env.UPSTREAM ?? "http://127.0.0.1:4000" });
gateway.server.listen(port, "127.0.0.1", () => {
  console.log(JSON.stringify({ level: "info", msg: "listening", port }));
});
for (const signal of ["SIGTERM", "SIGINT"] as const) {
  process.once(signal, () => {
    void gateway.shutdown(10_000).then(() => process.exit(0));
  });
}
