# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a product catalog API whose limits come from
   configuration, with validation, a service bean and Problem Details for every error.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Beans and Injection | How does the service get the configuration? Why constructor injection? |
| Configuration | How is a `@ConfigurationProperties` record registered and validated? Which source wins? |
| REST Controllers | Which status for create, missing, conflict? Path variable or request parameter? |
| Validation and Problem Details | Where do constraints go? How do domain errors become responses? |

> [!TIP]
> One grading test starts the application with other settings. If a limit is hard-coded,
> that test finds it.
