---
from: java
status: draft
concept: "Configuration without rebuilding"
from_way: "Properties files loaded by hand with Properties.load, one file per environment baked into the build, and @Value strings in every class."
native_way: "One application.yaml with profiles, overridden by environment variables, and read into validated @ConfigurationProperties records."
sensei_tip: "Write the properties record first and let validation describe the valid values; it doubles as documentation of every setting."
---

| Before | Spring Boot |
| --- | --- |
| `props.load(getClass().getResourceAsStream("/app.properties"))` | `application.properties` loaded automatically |
| `Integer.parseInt(props.getProperty("max"))` | `int maxItems` in a properties record |
| `config-prod.properties` chosen by the build | `application-prod.yaml` + `SPRING_PROFILES_ACTIVE=prod` |
| `@Value("${shop.currency}") String currency;` in many classes | `ShopProperties shop` injected once |
