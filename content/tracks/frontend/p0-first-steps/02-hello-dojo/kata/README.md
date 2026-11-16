# Hello, Dojo: a Business Card Page

Build a one-page business card with **semantic HTML**, a little **CSS** and a few lines of
**JavaScript**. The dojo loads `index.html` in a real headless Chromium, so what the tests
measure is what a browser really does.

**`index.html`** (edit the parts marked `todo`):

- `<html lang="en">` and a non-empty `<title>`.
- A `<main>` holding one `<article class="card">` with:
  - one `<h1>` with your name (any name);
  - a `<p class="role">` with what you do;
  - a `<ul>` with at least two `<li><a href="...">` links (a site, a `mailto:`...);
  - a `<button type="button" id="say-hello">Say hello</button>`;
  - an `<output id="greeting">`, empty at first.
- Valid HTML: html-validate must find no problems, and axe-core no accessibility problems
  (links need text, colors need enough contrast).

**`style.css`**: the card is at most `24rem` wide and centered on a wide screen, and on a
320px-wide phone it stays on screen without a horizontal scrollbar.

**`app.js`**:

| Code | What it does |
| --- | --- |
| `greeting(name)` | `"Hello from Ada Lovelace!"`; the name is trimmed, and a blank or missing name gives `"Hello from the dojo!"` |
| the click listener | Clicking the button (or pressing Enter on it) puts `greeting(<text of the h1>)` in the output |

Set the text with `textContent`, not `innerHTML`.

To look at the page in your own browser, run this in the kata folder and open
<http://localhost:8080/>:

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 11 tests: 1 in `tests/visible.test.js` and 10 hidden ones.
