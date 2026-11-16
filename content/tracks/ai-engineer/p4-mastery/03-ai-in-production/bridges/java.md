---
from: java
status: draft
concept: "Operate it for years, not weeks"
from_way: "Library upgrades with deprecation warnings, Flyway migrations, canary releases and incident runbooks for Spring services."
native_way: "Model and prompt changes handled like schema migrations: versioned, evaluated, rolled out gradually with a rollback, and backed by runbooks for AI-specific incidents."
sensei_tip: "Treat a model retirement like a Java LTS end-of-life: put the date in the plan, test the upgrade in CI against your suite early, and never let it become an emergency."
---

| Java operations | AI feature operations |
| --- | --- |
| Flyway migration | a reviewed prompt or model change |
| deprecation warnings | provider deprecation schedules |
| canary release | shadow traffic and staged rollout |
| Togglz or Unleash flags | switching prompt and model versions |
| runbooks and postmortems | runbooks for outages, regressions and harmful output |
