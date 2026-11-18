// Starts the API on http://127.0.0.1:3000 (npm start). You do not need to change it.
//   curl -i -X POST localhost:3000/polls -H "content-type: application/json" \
//        -d '{"question":"Tabs or spaces?","options":["Tabs","Spaces"]}'
import { createApp } from "./polls.ts";

const port = Number(process.env.PORT ?? 3000);
createApp().listen(port, "127.0.0.1", () => console.log(`polls API on http://127.0.0.1:${port}`));
