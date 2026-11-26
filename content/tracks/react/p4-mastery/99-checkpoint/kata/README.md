# Checkpoint 4: The Comment Thread (final exam)

A blog's comment thread, from the server function down to the button. It brings the whole track
together: pure checks on the server side, what may cross to the browser, Suspense and `use`,
an error boundary that reports, transitions, and an optimistic comment that rolls back.

`types.ts` and `cache.ts` are given (the cache is the one from the Resource Loader kata: `read`
returns the same promise until `evict`). Do not change them.

## 1. The server side (`server.ts`)

In a framework these would run behind `'use server'` and in a Server Component. Here they are
plain functions.

`checkNewComment(session, form, postIds)` checks a submitted form, in this order:

| Case | Result |
| --- | --- |
| `session` is `null` | `{ ok: false, error: "Sign in to comment" }` |
| `postId` is missing or not in `postIds` | `{ ok: false, error: "Unknown post" }` |
| `body` is missing or blank after trimming | `{ ok: false, error: "Write a comment first" }` |
| the trimmed body is longer than 280 characters | `{ ok: false, error: "Comments are at most 280 characters" }` |
| otherwise | `{ ok: true, comment: { postId, authorId, authorName, body } }` with the trimmed body |

The author **always** comes from the session. A form is untrusted input: extra fields such as
`authorId` or `authorName` are ignored.

`toPublicComments(rows)` turns rows of the comments table into what a Server Component may pass
to the browser: only `{ id, author, body }` (`author` is `authorName`; the e-mail and the
author id never leave the server), without deleted rows (`deletedAt` not `null`), oldest
`postedAt` first (the times may have different offsets). Do not reorder `rows`.

## 2. The error boundary (`ErrorBoundary.tsx`)

`<ErrorBoundary fallback={(error, reset) => ...} onError={...} resetKey={...}>` renders the
fallback when a child throws, calls `onError(error)` **once** per caught error, resets itself
when `resetKey` changes, and `reset()` clears the error.

## 3. The page (`Thread.tsx`)

`<Thread posts loadComments sendComment report />`:

- `<nav aria-label="Posts">` with one button per post title; `aria-pressed` tells the current
  post (the first one at the start).
- `<section aria-label="Discussion">` with the current post's comments, read with `use` from
  **one** cache for the life of the page (a post that was loaded once is never loaded again).
  - while the first load is pending: `<p role="status">Loading comments…</p>`;
  - switching posts happens in a **transition**: the old comments stay on screen and the
    section has `aria-busy="true"` until the new ones arrive (no loading fallback);
  - `<ul aria-label="Comments">` whose items read `Ana: Finally it clicks`, or
    `<p>No comments yet</p>`;
  - when loading fails: `<div role="alert">` with `Could not load comments` and a
    **Try again** button that loads that post again. The error is passed to `report` once,
    the rest of the page keeps working, and opening another post shows that post.
- Under the comments, `<form aria-label="New comment">` with a textarea labelled **Comment**
  and a **Post comment** button:
  - a blank comment is not sent: the textarea gets `aria-invalid="true"`, `aria-describedby`
    points to a text `Write a comment first`, and the focus goes to the textarea;
  - otherwise `sendComment(postId, trimmedBody)` is called, the textarea is emptied, the
    comment shows **at once** at the end of the list as `You: <body> (sending)`, and
    **Post comment** is disabled until the server answers;
  - on success the comment the server answered replaces it, and stays there when you switch
    posts and come back (without loading the post again);
  - on failure it disappears, the form shows `<p role="alert">Could not post your comment.
    Try again.</p>`, the textarea gets the text back, and the error goes to `report`.

There are 14 tests: 1 in `tests/visible.test.tsx` and 13 hidden ones. Run them with
`npm install` once, then `npm test`.
