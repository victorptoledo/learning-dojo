---
from: java
status: draft
concept: "Expose a new version to a few users first"
from_way: "A release goes to every application server in one maintenance window, with a database migration script run just before."
native_way: "A canary takes a small share of traffic, metrics decide, and Flyway migrations follow expand and contract so old and new code coexist."
sensei_tip: "Togglz or OpenFeature's Java SDK give you flags; Flyway's versioned migrations make expand and contract explicit."
---

| Java world | Release strategies |
| --- | --- |
| a maintenance window | a canary or blue-green switch |
| `if (config.isNewCheckout())` | a feature flag |
| a Flyway migration | an expand or contract step |
| redeploying the old WAR | weight back to 0 |
