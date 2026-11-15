---
from: javascript
status: draft
concept: "Automated regression hunting"
from_way: "Trying older npm versions of your own package one by one."
native_way: "git bisect run with a script that installs the commit's dependencies and runs one test."
sensei_tip: "Dependencies change across the range, so the script must run npm ci (or skip with 125 if it fails) before the test. A stale node_modules gives wrong answers silently."
---

| Node habit | With bisect |
| --- | --- |
| `npx my-lib@2.3` vs `@2.4` | `git bisect start v2.4.0 v2.3.0` |
| `npm test -- totals.test.js` | the run command |
| `npm ci` fails on an old commit | `exit 125` |
| Jest exit code 1 on failure | bad, as bisect expects |
