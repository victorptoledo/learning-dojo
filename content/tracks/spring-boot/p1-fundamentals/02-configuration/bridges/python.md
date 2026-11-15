---
from: python
status: draft
concept: "Configuration without rebuilding"
from_way: "settings.py, environment variables and pydantic-settings classes."
native_way: "application.properties or .yml, profiles, environment variables and @ConfigurationProperties records."
sensei_tip: "A @ConfigurationProperties record with @Validated is a pydantic BaseSettings class: typed, validated at startup, filled from properties and environment variables."
---

| Python | Spring Boot 4 |
| --- | --- |
| pydantic BaseSettings | @ConfigurationProperties record |
| os.environ['SMTP_HOST'] | SMTP_HOST maps to smtp.host |
| settings per environment | profiles: application-prod.yml |
| .env file | environment variables or spring.config.import |
