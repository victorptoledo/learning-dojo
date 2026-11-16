---
from: python
status: reviewed
concept: "From a server you SSH into to immutable releases"
from_way: "git pull on the server, pip install, restart gunicorn, and settings.py with secrets in it."
native_way: "An image per commit, settings from environment variables, secrets from the platform, migrations as a release step, and the same image in every environment."
sensei_tip: "In serverless Python, open database connections at module level so warm invocations reuse them, and still put a pooler in front of PostgreSQL."
---

| Python habit | Deployment practice |
| --- | --- |
| secrets in `settings.py` | environment variables and secret files |
| `migrate` on every container start | one migration job per release |
| `git pull` in production | promote an immutable image |
| one connection per Lambda | a pooler such as RDS Proxy or PgBouncer |
