---
from: javascript
status: draft
concept: "Where does this code run?"
from_way: "Your React code always ran in the browser; the server was a separate Express or API project."
native_way: "With RSC some components run only on the server (with database access), some on the client, and server functions are called like functions but run as endpoints."
sensei_tip: "For every file, ask: does it run on the server, the client or both? Secrets and database code must never be reachable from a 'use client' module."
---

| Client-only React | With RSC |
| --- | --- |
| `useEffect` + `fetch("/api/x")` | `await db.query()` in a Server Component |
| Express route for a POST | `"use server"` function |
| Everything in the bundle | Server Component code stays on the server |
| `ReactDOM.createRoot` | `hydrateRoot` on server HTML |
| API key in `.env` exposed by the bundler | Kept in server-only modules |
| One render location | Server and client, split by `"use client"` |
