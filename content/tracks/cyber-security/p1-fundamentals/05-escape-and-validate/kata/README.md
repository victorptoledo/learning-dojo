# Escape and Validate

`comments.py` renders the comments of a blog by pasting user data into HTML strings. Make
each function safe. Use the standard library only (`html`, `json`, `re`, `urllib.parse`).

| Function | Does |
| --- | --- |
| `validate_username(value) -> str` | Returns `value` unchanged if it is a `str` of 3 to 30 characters, each an ASCII letter, digit, `_`, `.` or `-`; otherwise raises `ValueError`. The **whole** value must match: `"ada\n"` is invalid. |
| `safe_link(url) -> str \| None` | Returns the URL with surrounding spaces removed if, after parsing, its scheme is `http` or `https` (any case) and it has a host; otherwise `None`. A URL containing any control character (code below 32, or 127) anywhere is `None`. |
| `render_comment(author, body, website=None) -> str` | The comment as HTML, below. |
| `render_search_box(query) -> str` | `<input type="search" name="q" value="QUERY">` with the query encoded for a quoted attribute. |
| `json_for_script(data) -> str` | `json.dumps(data)` made safe to place inside a `<script>` element: every `<`, `>` and `&` becomes `\u003c`, `\u003e`, `\u0026`, and the line separators U+2028 and U+2029 become `\u2028` and `\u2029`. The result still parses with `json.loads` to the same data. |

`render_comment` returns one line:

```html
<article class="comment"><h3>AUTHOR</h3><p>BODY</p>LINK</article>
```

- `AUTHOR` and `BODY` are encoded with `html.escape` (quotes included). After encoding, each
  newline in the body becomes `<br>`.
- `LINK` is `<a href="URL" rel="nofollow noopener noreferrer">website</a>` when `website` is
  given and `safe_link` accepts it, with the URL encoded for the attribute; otherwise empty.

```python
render_comment("Ada", "1 < 2 & 3 > 2", "https://example.org/?a=1&b=2")
# '<article class="comment"><h3>Ada</h3><p>1 &lt; 2 &amp; 3 &gt; 2</p>'
# '<a href="https://example.org/?a=1&amp;b=2" rel="nofollow noopener noreferrer">website</a></article>'
safe_link(" JavaScript:alert(1)")   # None
```

Run the visible tests with `uv run pytest`. There are 12 tests: 2 in
`tests/test_visible.py` and 10 hidden ones, plus ruff on `comments.py`.
