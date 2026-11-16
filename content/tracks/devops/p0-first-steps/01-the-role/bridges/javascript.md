---
from: javascript
status: draft
concept: "Developers own their service in production"
from_way: "Front-end work ends at `npm run build`; a Vercel or Netlify button, or another team, makes it live."
native_way: "You own the whole path: the build, the image or bundle, the deploy pipeline, the CDN and the alerts when it breaks."
sensei_tip: "The preview-deployment-per-pull-request habit from Vercel is exactly what DevOps teams build for back-end services too."
---

| JavaScript world | DevOps |
| --- | --- |
| `npm ci && npm test` | the CI job every pull request runs |
| a preview URL per PR | an ephemeral environment per PR |
| `package-lock.json` | pinned, reproducible builds |
| Sentry errors | observability and on-call |
