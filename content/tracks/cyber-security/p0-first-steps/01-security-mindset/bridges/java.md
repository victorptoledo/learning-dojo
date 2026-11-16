---
from: java
status: draft
concept: "Thinking in threats, not only in features"
from_way: "Trusting the framework: Spring Security is on the classpath, so the app must be secure."
native_way: "Name the assets and threats first, then check that each control (authentication, authorisation, validation, logging) is really applied where the threat is."
sensei_tip: "Least privilege applies to the JVM too: run the service as a non-root user, give its database account only the grants it needs, and keep actuator endpoints off the public network."
---

| Java | Security mindset |
| --- | --- |
| permitAll() left from prototyping | deny by default, open endpoints one by one |
| catch (Exception e) {} around a check | fail closed: deny and log |
| one DB user for the app and migrations | separate accounts, least privilege |
| debug logging of request bodies | never log passwords, tokens or card data |
| "the framework escapes it" | know which context it escapes, and where it does not |
