# Rendering UI: Server Pages, SPAs, Forms and Escaping

Data from the database ends up as pixels. Where the HTML is produced, on the server or in the
browser, shapes performance, complexity and security. Phase 2 covers the hybrid strategies
(SSR, SSG, hydration, server components); this lesson is about the two basic models and the
parts every model must get right.

## Server-rendered pages

The server runs a template (Thymeleaf, Razor, Jinja, Go's `html/template`, EJS, or plain
template literals with escaping) and sends finished HTML:

```text
GET /notes          →  200 text/html  (the list, already in the page)
POST /notes         →  303 Location: /notes        (or 422 with the page and the errors)
```

- **First paint is fast**: the HTML arrives with the content; search engines read it.
- **Little JavaScript**, so little to break; links and forms work everywhere.
- Each interaction reloads a page, unless you add small enhancements (fetch for one widget,
  libraries such as htmx that swap fragments).

## Single-page apps

The server sends a small HTML shell and a JavaScript bundle; the bundle calls JSON APIs and
builds the DOM (React, Vue, Angular, Svelte):

- **Rich interaction**: drag and drop, optimistic updates, offline drafts, no reloads.
- **More to own**: routing, loading and error states, caching, the bundle size, and a blank
  screen until the JavaScript runs.
- Two deployables (the static bundle and the API) and a contract between them.

**Choose per screen.** Public, content-heavy pages favour server rendering or static
generation; tool-like screens behind a login favour an SPA. Many products have both.

## Forms, end to end

The same rules hold whether a form posts natively or through `fetch`:

1. **Validate on the server**, with the same rules for every entry point (the form and the JSON
   API share one validation function in the next checkpoint).
2. On failure, show the page again with **`422`**, each message **next to its field**, and
   **what the user typed** still in the fields. Losing a long text on a typo is the fastest way
   to lose a user.
3. On success, answer **`303 See Other`** to the result page (Post/Redirect/Get), so reload and
   back do not resubmit.
4. In an SPA, map the API's problem details (`errors: [{ path, message }]`) to the fields, and
   disable the submit button while the request runs to avoid double submissions.

## UI state

Every screen that loads data has at least these states, and each needs a design: **empty** ("No
notes yet"), **loading**, **error** (with a retry), **partial** (some data, more loading) and
**success**. Keep server data (what the API says) separate from UI state (which dialog is open,
what is being typed): mixing them is the root of many stale-data bugs, and the data-fetching
lesson of phase 2 deals with server data properly.

## Escaping is contextual

Escaping depends on **where** the text goes:

| Context | Example | What to do |
| --- | --- | --- |
| HTML text | `<p>${name}</p>` | escape `& < >` (and quotes) |
| Attribute | `<input value="${name}">` | escape quotes too, always quote attributes |
| URL | `<a href="${url}">` | allow only `http:`/`https:`; `javascript:` URLs run code |
| JavaScript | `<script>const x = ${data}</script>` | avoid; pass data in a `data-` attribute or a JSON script tag, properly encoded |

Template engines and React escape text by default. The holes are the escape hatches:
`th:utext`, `@Html.Raw`, `| safe`, `dangerouslySetInnerHTML`, `v-html`. Treat each one as a
security review item.

A **Content-Security-Policy** header (`default-src 'self'; script-src 'self'`) tells the browser
to refuse inline scripts and scripts from other origins, so a missed escape is much harder to
exploit.

## Summary

- Server rendering: fast first paint, little JavaScript. SPAs: rich interaction, more to own.
  Choose per screen.
- Forms: validate on the server, 422 with errors under fields and the typed values, 303 on success.
- Design every state: empty, loading, error, partial, success.
- Escape for the context, review every raw-HTML escape hatch, and add a CSP.

## Further reading

- web.dev, "Rendering on the Web": <https://web.dev/articles/rendering-on-the-web>
- MDN, "Client-side form validation": <https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Forms/Form_validation>
- OWASP, "Cross Site Scripting Prevention Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html>
- MDN, "Content Security Policy (CSP)": <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CSP>
- In the dojo: the Frontend track's *Forms* and *Browser Security*, and the React track's *JSX and Function Components* and *Forms*.
