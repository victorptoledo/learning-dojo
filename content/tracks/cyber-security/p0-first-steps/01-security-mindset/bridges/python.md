---
from: python
status: draft
concept: "Thinking in threats, not only in features"
from_way: "Shipping with DEBUG = True, a SECRET_KEY in settings.py and the admin at /admin."
native_way: "Secure defaults: debug off, secrets from the environment or a vault, admin surfaces restricted, and a list of what can go wrong for each feature."
sensei_tip: "Django's `manage.py check --deploy` and a dependency audit (pip-audit) turn several secure-default questions into a checklist you can run in CI."
---

| Python | Security mindset |
| --- | --- |
| DEBUG = True in production | secure defaults, debug only locally |
| except Exception: pass around auth | fail closed |
| pickle.loads on request data | never deserialise untrusted data into objects |
| SECRET_KEY committed to git | secrets outside the code, rotated |
| a superuser DB connection | least privilege |
