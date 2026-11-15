---
from: javascript
status: draft
concept: "Configuration without rebuilding"
from_way: "dotenv files and process.env, or NestJS ConfigModule with validation."
native_way: "Properties files, profiles and typed, validated configuration classes."
sensei_tip: "Spring's relaxed binding maps MAIL_HOST to mail.host automatically, like a config module that reads both files and the environment, with the environment winning."
---

| JavaScript / TypeScript | Spring Boot 4 |
| --- | --- |
| process.env.MAIL_HOST | mail.host (from MAIL_HOST) |
| .env.production | application-prod.properties |
| ConfigModule + Joi | @ConfigurationProperties + @Validated |
| NODE_ENV | spring.profiles.active |
