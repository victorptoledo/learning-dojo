---
from: java
status: draft
concept: "Signing and secrets"
from_way: "Signing JARs with jarsigner and artefacts with GPG for Maven Central, while application.properties holds the database password."
native_way: "Sign commits and tags the same way artefacts are signed, and keep secrets out of application.properties: environment variables or a secret manager, with Spring's ${DB_PASSWORD} placeholders."
sensei_tip: "Commit application.properties with placeholders such as spring.datasource.password=${DB_PASSWORD} and keep real values in the environment. A pre-commit scanner then has nothing to find."
---

| Java habit | Git practice |
| --- | --- |
| GPG-signed artefacts for Maven Central | `tag.gpgsign true` for release tags |
| password in `application.properties` | `${DB_PASSWORD}` placeholder |
| `settings.xml` with repository credentials | stays in `~/.m2`, never in the project |
| leaked keystore in history | rotate, then `git filter-repo --invert-paths --path` |
