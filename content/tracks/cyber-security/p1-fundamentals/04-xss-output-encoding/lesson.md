# Input Validation, Output Encoding and XSS

**Cross-site scripting (XSS)** is injection into a web page: the attacker's text ends up in
HTML that a victim's browser runs as script. The script runs with the victim's session, on
your origin, so it can read the page, call your API as the victim, change their email
address, or show a fake login form. XSS is part of A05 Injection in the OWASP Top 10.

## Three forms

| Form | How the payload arrives | Example |
| --- | --- | --- |
| **Reflected** | in the request, echoed straight back | `/search?q=<script>...</script>` sent in a link |
| **Stored** | saved by the app, shown to others later | a comment or display name with a payload |
| **DOM-based** | front-end code moves data into a dangerous sink | `el.innerHTML = location.hash.slice(1)` |

## Validation and encoding are different jobs

**Input validation** checks that data has the shape you expect, as early as possible:

- Use an **allow-list** (what is allowed), not a deny-list (`<script>` filters are always
  bypassed: `<img src=x onerror=...>`, `<svg onload=...>`, mixed case, encodings).
- Match the **whole** value. In Python `re.fullmatch`; with `re.match(r"^[a-z]+$", s)` the `$`
  also matches before a final newline, so `"abc\n"` passes.
- Canonicalise first (Unicode NFKC, decode once), then validate.
- Parse structured values with a real parser: URLs with `urllib.parse`, dates with
  `datetime`, numbers with `int`.

Validation reduces what reaches your code, but many valid values contain dangerous characters:
`O'Brien`, `Tom & Jerry`, a math comment with `<`. So the real defence against XSS is
**output encoding**: when you write data into a page, encode it for the **context** it lands in.

| Context | Example | Encode |
| --- | --- | --- |
| HTML body | `<p>{name}</p>` | `&` `<` `>` as entities |
| HTML attribute | `<input value="{q}">` | the above plus `"` and `'`, and always quote the attribute |
| URL in `href`/`src` | `<a href="{url}">` | check the scheme first (`javascript:` is a URL), then attribute-encode |
| JavaScript data | `<script>const d = {json}</script>` | JSON, with `<`, `>`, `&` written as `\u003c` and friends so `</script>` cannot close the tag |
| CSS | `style="color: {c}"` | avoid; use an allow-list of values |

In Python, `html.escape(value, quote=True)` handles the HTML body and quoted attributes.
Template engines do this for you: Jinja2 with autoescape, Django templates, Thymeleaf, Razor,
React's JSX. XSS comes back when you turn that off: `|safe`, `Markup()`, `th:utext`,
`@Html.Raw`, `dangerouslySetInnerHTML`, `v-html`, `innerHTML`.

> [!WARNING]
> Encoding for one context is not safe in another. `html.escape` does nothing useful inside
> a `<script>` block or an event handler attribute like `onclick="..."`. Do not put user
> data there at all.

## When users may write HTML

For rich text (a Markdown bio, a WYSIWYG editor) escaping would destroy the formatting.
Instead, render it, then **sanitise** the HTML with an allow-list library that parses it and
keeps only known-safe tags and attributes: DOMPurify in the browser, `nh3` (Python, binding to
the Rust `ammonia` crate), OWASP Java HTML Sanitizer, HtmlSanitizer for .NET, bluemonday for
Go. Never write your own sanitiser with regular expressions.

## Content Security Policy: the second line

A **CSP** response header tells the browser where scripts may come from. A strict policy
blocks most injected scripts even when an encoding bug slips through:

```text
Content-Security-Policy: default-src 'self'; script-src 'self' 'nonce-r4nd0m'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'
```

Inline `<script>` blocks without the per-response **nonce** and inline event handlers do not
run. `'unsafe-inline'` in `script-src` gives this protection away. Phase 3 covers CSP and the
other security headers in depth.

Also set session cookies `HttpOnly`, so script cannot read them. That limits the damage of
XSS (the token is not stolen) but does not prevent it (the script can still act as the user).

## Summary

- XSS is injection into HTML: reflected, stored or DOM-based.
- Validate input with allow-lists and full matches; encode output for its exact context.
- Keep template auto-escaping on; review every "raw" escape hatch.
- Sanitise user HTML with a real allow-list sanitiser; add a strict CSP as defence in depth.

## Further reading

- OWASP Cross Site Scripting Prevention Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html>
- OWASP Input Validation Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html>
- OWASP DOM based XSS Prevention Cheat Sheet: <https://cheatsheetseries.owasp.org/cheatsheets/DOM_based_XSS_Prevention_Cheat_Sheet.html>
- MDN, Content Security Policy: <https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CSP>
- Python `html.escape`: <https://docs.python.org/3/library/html.html#html.escape>
