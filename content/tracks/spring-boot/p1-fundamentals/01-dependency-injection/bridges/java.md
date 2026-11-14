---
from: java
status: draft
concept: "Wiring objects"
from_way: "A main method full of new calls, static singletons with getInstance(), or field injection with @Autowired on private fields."
native_way: "Beans declared with stereotypes or @Bean methods, created once by the container, and handed to final fields through the constructor."
sensei_tip: "If a class has more than four or five constructor parameters, it is doing several jobs: split it before reaching for setters or field injection."
---

| Before | Spring Boot |
| --- | --- |
| `Mailer.getInstance()` | `Mailer` as a bean, injected where needed |
| `@Autowired private Repo repo;` | `private final Repo repo;` + constructor |
| `if (type.equals("card")) new CardPayment()` | `Map<String, PaymentMethod>` injected by name |
| Initialisation code in `main` | `@PostConstruct` or `ApplicationRunner` |
