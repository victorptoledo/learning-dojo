// Starts the API on http://127.0.0.1:3000 (npm start). You do not need to change it.
//   curl -i -X POST localhost:3000/notes -H "content-type: application/json" -d '{"text":"hi"}'
import { createApp } from "./app.ts";

const port = Number(process.env.PORT ?? 3000);
createApp().listen(port, "127.0.0.1", () => {
  console.log(`notes API on http://127.0.0.1:${port}`);
});
