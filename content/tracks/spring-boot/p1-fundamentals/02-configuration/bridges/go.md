---
from: go
status: draft
concept: "Configuration without rebuilding"
from_way: "flag, os.Getenv, or viper and envconfig."
native_way: "Layered property sources with profiles and typed binding."
sensei_tip: "viper's layered config (defaults, file, env) is Spring's property source order. Bind to a record and validate it, instead of reading strings everywhere."
---

| Go | Spring Boot 4 |
| --- | --- |
| os.Getenv("MAIL_HOST") | MAIL_HOST bound to mail.host |
| viper.SetDefault | defaults in application.yml |
| envconfig struct | @ConfigurationProperties record |
| flags | --mail.host=... command-line arguments |
