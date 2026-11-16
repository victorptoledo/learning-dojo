---
from: javascript
status: draft
concept: "Everything in AWS is an API you call with signed credentials"
from_way: "A Node.js app runs on Heroku or a VPS; the platform or you handle the OS."
native_way: "The AWS SDK for JavaScript v3 calls AWS APIs; the shared responsibility line moves with the service you pick."
sensei_tip: "Import only the clients you use (`@aws-sdk/client-s3`) and let `fromNodeProviderChain()` (the default) find credentials; never bundle keys into a front end."
---

| Node.js world | AWS |
| --- | --- |
| `.env` with keys | SSO profiles locally, roles in AWS |
| Heroku dynos | EC2, ECS, Lambda: pick the responsibility you want |
| `npm run deploy` scripts | infrastructure as code (Phase 1) |
| one hosting region | an AWS region chosen by law, latency and price |
