---
from: javascript
status: draft
concept: "Shipping Java today"
from_way: "pino logs, dotenv, pm2 or containers, and prom-client metrics."
native_way: "SLF4J, environment configuration, a JAR in a JRE image, Micrometer metrics."
sensei_tip: "Log with placeholders (log.info(\"order {}\", id)), not string concatenation: the string is only built when the level is enabled, like pino's lazy serialisation."
---

| JavaScript / TypeScript | Java 25 |
| --- | --- |
| pino | SLF4J + Logback |
| dotenv | environment variables or config files |
| node:22-slim | eclipse-temurin:25-jre |
| prom-client | Micrometer |
| process.on('SIGTERM') | shutdown hooks |
