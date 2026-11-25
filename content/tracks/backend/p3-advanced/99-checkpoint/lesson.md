# Checkpoint 3: Advanced

This checkpoint closes the **Advanced** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a statements API made ready to run on many
   instances. You close the OWASP holes (object-level authorization, mass assignment, SQL
   injection, unbounded pages), put retries, a circuit breaker and a shared last-known value
   around a flaky rates service, and measure an availability SLO with its error budget and burn
   rate.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Scaling a Backend | What must not live in one instance's memory once there are many? What do replicas cost in consistency? |
| API Security: The OWASP API Top 10 | Is the object the caller's? Which fields may the client set? Is every value bound, and every list bounded? |
| Resilient Client (kata) | Which failures are worth a retry? What does an open circuit do, and when does it try again? |
| SRE: SLIs, SLOs and Incidents | Which answers burn the budget? How fast is it burning, and when do you page someone? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
