---
from: java
status: reviewed
concept: "Layered configuration"
from_way: "Spring Boot's property sources: application.yml, profiles, environment variables and command-line arguments, later ones overriding earlier ones."
native_way: "Defaults, postgresql.conf, ALTER SYSTEM, database, role, session, transaction and function settings, later ones winning; pg_settings.source says which one did."
sensei_tip: "Like /actuator/env telling you which property source won, pg_settings tells you where a value came from. Check it before you trust a setting."
---

| Java / Spring | PostgreSQL |
| --- | --- |
| application.yml | postgresql.conf |
| --spring.x=y on the command line | postgres -c x=y |
| a profile for the reporting job | ALTER ROLE reporting SET work_mem = '256MB' |
| -Xmx | shared_buffers (restart needed) |
| spring.datasource.hikari.connection-timeout | statement_timeout, lock_timeout per role |
| /actuator/env | SELECT name, setting, source FROM pg_settings |
