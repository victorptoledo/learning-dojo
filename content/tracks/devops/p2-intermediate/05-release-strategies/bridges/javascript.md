---
from: javascript
status: draft
concept: "Expose a new version to a few users first"
from_way: "An A/B test shows a new UI to half of the users, chosen by a cookie."
native_way: "A canary does the same for a whole service version, by traffic weight or header, and promotion depends on reliability metrics."
sensei_tip: "Sticky assignment matters: hash the user ID so one user does not bounce between versions on each request."
---

| JavaScript world | Release strategies |
| --- | --- |
| an A/B test | a canary |
| a cookie-based split | a header or weight route |
| a LaunchDarkly flag | a feature flag |
| a rollback deploy | weight back to 0 |
