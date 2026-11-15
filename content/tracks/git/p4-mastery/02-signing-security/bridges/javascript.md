---
from: javascript
status: draft
concept: "Signing and secrets"
from_way: "An .npmrc with an auth token, or an .env committed with the frontend's API keys."
native_way: "Tokens in CI secrets and the user's ~/.npmrc, .env ignored, and anything bundled into a frontend treated as public."
sensei_tip: "Anything in a browser bundle is public, ignored file or not: a VITE_ or NEXT_PUBLIC_ variable is shipped to every visitor. Only server-side code may hold real secrets."
---

| Node habit | Git practice |
| --- | --- |
| project `.npmrc` with `_authToken` | token in `~/.npmrc` or CI secrets |
| `.env` committed | ignored; `.env.example` committed |
| npm provenance for packages | signed build attestations, like signed tags |
| leaked token | revoke on npm/GitHub first, then clean history |
