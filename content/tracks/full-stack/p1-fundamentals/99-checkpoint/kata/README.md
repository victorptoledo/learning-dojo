# Checkpoint 1: a Notes App from Database to Page

Build a small notes app end to end, with no framework: a SQLite table, one validation shared by
the HTML form and the JSON API, a server-rendered page with a search box, and a form that
follows the Post/Redirect/Get pattern. Everything runs on Node.js 26: `node:sqlite`, `node:http`.

## `notes.ts`: one validation for every entry point

`validateNote(input)` takes whatever arrived (a parsed form or a JSON body) and returns
`{ ok: true, note: { title, body } }` or `{ ok: false, errors }`:

| Field | Rule | Error message (in `errors.title` / `errors.body`) |
| --- | --- | --- |
| `title` | a string, trimmed, 1 to 80 characters | `"Title is required"` (missing, not a string or blank), `"Title must be at most 80 characters"` |
| `body` | optional; a string, trimmed, at most 2000 characters; missing means `""` | `"Body must be at most 2000 characters"` (also when it is not a string) |

## `db.ts`: the repository

`openNotes(path = ":memory:")` opens a `DatabaseSync`, creates the table if it does not exist
(the schema is in the file) and returns a `NotesRepo`:

| Method | Does |
| --- | --- |
| `create(note)` | Inserts it and returns the stored `Note` (`id`, `title`, `body`) |
| `list(search?)` | Every note, **newest first** (highest id first). With a search text, only the notes whose title contains it, ignoring ASCII case. `%` and `_` in the search are literal characters, not wildcards |
| `get(id)` | The note, or `undefined` |
| `remove(id)` | Deletes it; returns whether a note was deleted |

Use parameters (`?`) for every value: a title such as `it's'); DROP TABLE notes; --` is just a
title. Return plain objects (`{ id, title, body }`), not the driver's row objects.

## `views.ts`: the page

`notesPage({ notes, search, errors, values })` returns the HTML of the whole page. Every piece
of text that came from a user goes through `escapeHtml` (already written). The page must have:

- a search form: `<form method="get" action="/">` with `<input name="q" value="SEARCH">`;
- the new-note form: `<form method="post" action="/notes">` with `<input name="title" value="...">`
  and `<textarea name="body">...</textarea>`, filled with `values` when the page is shown again
  after an error (empty otherwise);
- for each error, `<p class="error">MESSAGE</p>`;
- for each note, in the given order:
  `<article class="note" data-id="ID"><h2>TITLE</h2><p>BODY</p>` and a delete form
  `<form method="post" action="/notes/ID/delete">` with a button, then `</article>`;
- when there are no notes, `<p class="empty">No notes yet</p>`.

## `app.ts`: the routes

`createApp(repo)` returns a `node:http` server (not listening). `readBody`, `sendHtml` and
`sendJson` are written for you.

| Request | Answer |
| --- | --- |
| `GET /` (and `GET /?q=text`) | `200` HTML, the page with `repo.list(q)` |
| `POST /notes` (a form, `application/x-www-form-urlencoded`) valid | `303 See Other`, `Location: /` (Post/Redirect/Get: a reload does not post again) |
| `POST /notes` invalid | `422` HTML, the page again with the errors and what the user typed; nothing stored |
| `POST /notes/ID/delete` | `303`, `Location: /`; an unknown id is a `404` HTML page |
| `GET /api/notes` (and `?q=`) | `200` JSON, the list |
| `GET /api/notes/ID` | `200` JSON, the note; unknown: `404` JSON `{"error": "not found"}` |
| `POST /api/notes` (JSON) | `201`, `Location: /api/notes/ID`, the note; invalid: `422` JSON `{"errors": {...}}`; a body that is not JSON: `400` JSON `{"error": "..."}` |
| anything else | `404` |

Try it: `npm start` serves the app on <http://localhost:3000> with the notes in `notes.db`.

## How the kata is checked

`tsc` checks every file, then `node:test` runs the repository against an in-memory database and
the app on `127.0.0.1`. There are 16 tests: 2 in `tests/visible.test.ts` and 14 hidden ones. The
functions that still throw `new Error("todo ...")` are yours to write.
