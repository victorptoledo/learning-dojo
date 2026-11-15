---
from: javascript
status: draft
concept: "Team workflows"
from_way: "Preview deployments for every branch, but branches that live for weeks."
native_way: "GitHub flow with preview deployments per pull request, short branches, and feature flags for anything bigger than a few days."
sensei_tip: "Preview deployments (Vercel, Netlify, Cloudflare Pages) let reviewers click through the change. Link the preview in the PR description and reviews get faster and better."
---

| Node / frontend habit | Workflow practice |
| --- | --- |
| long-lived `redesign` branch | flags (LaunchDarkly, Unleash, a config flag) |
| preview URL per branch | linked in the pull request |
| `npm test` and lint in CI | required checks before merge |
| many PRs a day on one repository | a merge queue |
