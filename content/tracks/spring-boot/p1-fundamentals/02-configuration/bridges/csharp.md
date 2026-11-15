---
from: csharp
status: draft
concept: "Configuration without rebuilding"
from_way: "appsettings.json, appsettings.Production.json, IOptions<T> with validation."
native_way: "application.yml, application-prod.yml, @ConfigurationProperties with validation."
sensei_tip: "This is a one-to-one map: IOptions<MailOptions> is a @ConfigurationProperties(\"mail\") record, ASPNETCORE_ENVIRONMENT is the active profile, and environment variables override both."
---

| C# | Spring Boot 4 |
| --- | --- |
| appsettings.json | application.yml |
| appsettings.Production.json | application-prod.yml |
| IOptions<T> + ValidateOnStart | @ConfigurationProperties + @Validated |
| Mail__Host env var | MAIL_HOST |
