# Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the whole track. It has two parts, and you
need both:

1. **The quiz** (press **Q**): eight questions over the whole track, weighted towards Suspense,
   transitions, Server Components and React in production. You need 7 right.
2. **The integration kata** (press **Enter**): a blog's comment thread, from the server
   function down to the button. You check untrusted input on the server, send only public
   fields to the browser, load comments with `use` and Suspense from a cache, contain and
   report failures with an error boundary, switch posts in a transition, and post comments
   optimistically with a rollback when the server says no.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Suspense, use, Transitions and Error Boundaries | Where do the promises come from? Which boundary shows its fallback, and when does a transition avoid it? |
| Resource Loader (kata) | Why must Try again evict the cache and render again? |
| Server Components, Server Functions and Frameworks | What may cross the `'use client'` boundary? What must a server function check, whatever the page did? |
| React in Production | How do errors reach you from users' browsers? What does a reporting boundary add? |
| Forms (P1) and Testing and Accessibility (P3) | Where does the focus go after an error, and how does a screen reader hear about it? |
| Reconciliation (P3) | Which state survives a switch, and which should reset? |

> [!TIP]
> If a quiz question surprises you, open the lesson it comes from again before retrying. The
> explanation after each answer says which idea it tests.
