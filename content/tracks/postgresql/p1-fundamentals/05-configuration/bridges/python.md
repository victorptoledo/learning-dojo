---
from: python
status: reviewed
concept: "Layered configuration"
from_way: "Django settings modules, environment variables and pydantic-settings, where later sources override earlier ones."
native_way: "Defaults, postgresql.conf, ALTER SYSTEM, database, role, session and transaction settings, later ones winning."
sensei_tip: "Set options per connection with psycopg's options='-c statement_timeout=30s', or better once per role with ALTER ROLE, so every client gets it."
---

| Python | PostgreSQL |
| --- | --- |
| settings.py defaults | postgresql.conf |
| os.environ override | ALTER SYSTEM / command line |
| a settings module per service | ALTER ROLE api SET ... |
| with override_settings(...): | SET LOCAL inside a transaction |
| connect(options='-c work_mem=64MB') | a session-level SET |
| print(settings.X) | SHOW x / pg_settings |
