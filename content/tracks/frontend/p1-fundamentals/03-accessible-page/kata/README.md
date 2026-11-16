# Accessible Page: Fix the Meetup Page

`index.html` is the page of a local meetup. It looks fine and works with a mouse, but a screen
reader user, a keyboard user or someone with low vision cannot use it. Fix it. The tests run
**axe-core** (the engine behind many accessibility checkers), **html-validate** and a set of
keyboard checks in a real browser.

What must be true when you are done:

1. **Language and landmarks.** `<html lang="en">`; one `<header>`, one `<nav>`, one
   `<main id="main">` and one `<footer>` instead of the `div`s.
2. **Headings.** One `<h1>`, then `<h2>` for the three sections: no level is skipped.
3. **Images.** The logo and the speaker's photo get a useful `alt` (describe the person, not
   the file). The divider is decoration: `alt=""`.
4. **A skip link.** The first thing `Tab` reaches is `<a href="#main">Skip to content</a>`. It
   is off screen until it has focus, visible while it has focus, and `Enter` jumps to `#main`.
   Keep it focusable: hide it with a transform or position, not `display: none`.
5. **The details toggle is a `<button type="button">`**, with `aria-controls="talk-details"` and
   `aria-expanded` that `app.js` keeps in step (`"false"` closed, `"true"` open). `Enter` and
   `Space` work for free on a real button.
6. **The form.** A `<label>` "Email" tied to an `<input type="email" autocomplete="email">`,
   and a `<button type="submit">Subscribe</button>`. Pressing `Enter` in the field submits it;
   the thanks message goes in an element with `role="status"` so it is announced.
7. **Visible focus.** Do not remove the focus ring: use `:focus-visible` to style it.
8. **Contrast.** The grey text needs at least 4.5:1 against its background.
9. **Links that make sense alone.** No "click here".

When axe-core finds a problem, the failure names the rule (for example `image-alt`) and the
element. Look the rule up at <https://dequeuniversity.com/rules/axe/>.

To look at the page in your own browser:

```bash
docker run --rm -p 8080:8080 -v "${PWD}:/work" dojo-web serve
```

There are 13 tests: 1 in `tests/visible.test.js` and 12 hidden ones.
