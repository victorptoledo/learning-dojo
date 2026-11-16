// Starts the service on http://127.0.0.1:3000 (npm start). You do not need to change it.
//   curl -X POST localhost:3000/import -H "content-type: text/csv" --data-binary @stock.csv
import { createInventoryServer } from "./inventory.ts";

const port = Number(process.env.PORT ?? 3000);
createInventoryServer().listen(port, "127.0.0.1", () => {
  console.log(`inventory service on http://127.0.0.1:${port}`);
});
