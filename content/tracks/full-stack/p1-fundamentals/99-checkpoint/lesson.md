# Checkpoint 1: Fundamentals

This checkpoint closes the **Fundamentals** phase. It has two parts, and you need both:

1. **The quiz** (press **Q**): ten questions over the whole phase. You need 8 right.
2. **The integration kata** (press **Enter**): a notes app from a SQLite table to a
   server-rendered page, with a search box, a Post/Redirect/Get form and a JSON API that share
   one validation.

Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| HTTP Between Browser and Server | Which status answers a successful form post? What do cookies' flags and CORS do? |
| Typed API Contracts | Where is input validated? What does a 422 body look like? |
| From Screens to Tables | Why parameters instead of string concatenation? How do keyset pages work? |
| Rendering UI | What does the page show after an invalid post? Which characters are escaped? |

> [!TIP]
> Write `validateNote` first and use it from both entry points: the form and the JSON API
> should differ only in how they parse the body and how they shape the answer.
