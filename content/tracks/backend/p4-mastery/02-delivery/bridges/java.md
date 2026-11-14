---
from: java
status: draft
concept: "Shipping changes"
from_way: "Monthly releases from a release branch, a maintenance window to deploy, ALTER TABLE in the deploy script, and long-lived feature branches."
native_way: "Trunk-based development, a fast pipeline that promotes one image, canary or blue-green deploys, expand-and-contract migrations and feature flags."
sensei_tip: "If deploying is scary, deploy more often: smaller changes are easier to verify and to roll back."
---

| Before | Now |
| --- | --- |
| Release every month | many small deploys a day |
| Maintenance window | rolling, blue-green or canary deploys |
| Rename column in one step | expand → migrate → switch → contract |
| Feature branch for weeks | merge daily behind a feature flag |
