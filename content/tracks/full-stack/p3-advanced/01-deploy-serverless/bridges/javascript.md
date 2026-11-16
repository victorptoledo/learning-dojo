---
from: javascript
status: reviewed
concept: "Shipping a front end and an API together"
from_way: "vercel deploy for the front end, a separate manual deploy for the API, and API keys in .env files bundled into the app."
native_way: "One pipeline builds the static bundle and the API image per commit, only public values go into the bundle, secrets stay on the server, and migrations run before the API version that needs them."
sensei_tip: "Anything prefixed NEXT_PUBLIC_ or VITE_ is shipped to every browser; grep your bundle for secrets before every release."
---

| Node.js habit | Deployment practice |
| --- | --- |
| `.env` committed | secrets from the platform |
| `VITE_API_KEY` | public values only in the bundle |
| `prisma migrate deploy` in `start` | a migration step before the release |
| serverless + Prisma | a pooler or a serverless driver |
