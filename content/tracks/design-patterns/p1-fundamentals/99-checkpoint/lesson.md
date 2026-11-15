# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a notification hub whose `switch` over channels
   becomes strategies, whose SMS library and mailer hide behind adapters, and whose duplicate
   suppression becomes a decorator that can wrap any channel.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Composition, Coupling and Cohesion | Why does a wrapper beat a subclass? Which coupling is this? Is this duplication knowledge or coincidence? |
| Strategy and Factory | Lambda or class? Simple factory, factory method or registry? |
| Shipping Rules (kata) | How do I add a method without editing the calculator? |
| Adapter, Decorator, Proxy and Facade | What is this wrapper **for**? Why does self-invocation skip `@Transactional`? |
| Payment Gateway (kata) | Which class may know the SDK? What changes when I swap two decorators? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
