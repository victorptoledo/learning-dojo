---
from: python
status: draft
concept: "Respond in roles, mitigate first, learn without blame"
from_way: "Sentry fills with errors and whoever sees it first starts poking at the server."
native_way: "The page goes to the on-call, an incident is declared, roles are assigned, and the first move is reverting what changed."
sensei_tip: "py-spy dump on a stuck worker takes a second and keeps evidence that a restart would erase."
---

| Python world | Incident response |
| --- | --- |
| a Sentry spike | a symptom alert |
| SSH and poke | runbook steps |
| `git revert` and deploy | the first mitigation |
| a retro | a postmortem with owners |
