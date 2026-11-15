# Checkpoint 2: Intermediate

This checkpoint closes the **Intermediate** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): eight questions over the whole phase. You need 7 right.
2. **The integration kata** (press **Enter**): a resilience kit with a `retry` decorator, a
   circuit breaker that is both a context manager and a decorator, and a timer, all tested
   with a fake clock.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Exceptions | What does `raise ... from error` add? Where do you catch broadly, and why only there? |
| Modules and Packaging | How many times does a module's top level run? What does the src layout protect you from? |
| Generators | When does a generator body start running? What happens when you iterate an iterator twice? |
| Decorators and Context Managers | How many levels does a decorator with arguments have? What does a true return from `__exit__` do? |
| Testing with pytest | What runs after `yield` in a fixture? Where do you patch a name? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
