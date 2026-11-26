# Checkpoint 5: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards CPython
   internals, typed services and production Python. You need 7 right.
2. **The integration kata** (press **Enter**): an orders API that works on a laptop and must
   now go live. Load validated settings that never leak the database password, log JSON lines
   with a request id, open the repository once in a lifespan, validate input strictly, keep the
   cost price inside, and turn crashes into a logged traceback and a one-line 500.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| CPython Internals | When is an object freed? What does the cycle collector add? Why is `is` wrong for comparing numbers? |
| Typed Web Services with FastAPI | Why separate input and output models? What does `Depends` give you, and when is an endpoint `def` instead of `async def`? Where do startup and shutdown go? |
| Python in Production | Where does configuration come from, and what happens when it is wrong? Why log to stdout as JSON? What should a 500 show the client, and what should it log? |
| The Standard Library at Work | How do you write a timestamp that every reader understands? |
| Earlier phases | Exceptions chained or not (`from None`), Protocols for dependencies, pytest fixtures. |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
