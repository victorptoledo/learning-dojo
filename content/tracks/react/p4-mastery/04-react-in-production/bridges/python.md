---
from: python
status: draft
concept: "Apps, linters and deprecation warnings"
from_way: "Django projects split code into apps by domain, enforce style with ruff and mypy, and upgrade by fixing DeprecationWarnings first."
native_way: "React projects split code into feature folders, enforce rules with TypeScript and ESLint, and upgrade by fixing console warnings and running codemods."
sensei_tip: "Treat React's console warnings like Python's DeprecationWarnings run with -W error in CI: fix them before the next major and the upgrade is boring."
---

| Python | React |
| --- | --- |
| Django apps by domain | Feature folders |
| mypy strict | TypeScript strict |
| ruff | ESLint (hooks, a11y) |
| `-W error::DeprecationWarning` | Fix every React warning before upgrading |
| Sentry for Django | Sentry (or similar) with source maps |
| pytest + Playwright | Testing Library + Playwright |
