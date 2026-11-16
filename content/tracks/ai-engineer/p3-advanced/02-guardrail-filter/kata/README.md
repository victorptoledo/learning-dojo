# Guardrail Filter

Write `guardrails.py`: the deterministic checks that sit before and after a model call. `llm.py`
(given) has `ToolCall`. `Redaction`, `Session`, `ToolRule`, `Decision` and `INJECTION_PATTERNS`
are given in `guardrails.py`.

## 1. Personal data

- `luhn_valid(number)`: `True` when the string of digits passes the Luhn checksum (from the right,
  double every second digit, subtract 9 from results above 9, the sum is a multiple of 10).
  Fewer than 13 or more than 19 digits, or any non-digit: `False`.
- `redact(text) -> Redaction(text, found)` replaces, in this order:
  1. **cards**: 13 to 19 digits, optionally separated by single spaces or hyphens
     (`4111 1111 1111 1111`, `4111-1111-1111-1111`), not part of a longer run of digits, and only
     when the digits pass `luhn_valid`. Others stay as they are;
  2. **emails**: `name@domain.tld` (letters, digits and `._%+-` before the `@`; letters, digits,
     `.` and `-` in the domain; a top-level domain of at least two letters);
  3. **phones** in international format: `+`, then digits, spaces or hyphens, 9 to 15 digits in all,
     ending with a digit (`+351 912 345 678`).

  Placeholders are `<CARD_1>`, `<EMAIL_1>`, `<PHONE_1>`, numbered per kind in order of appearance;
  the same value gets the same placeholder. `found` maps each placeholder to the original text.
- `restore(text, found)`: puts the original values back.

## 2. Untrusted content

- `detect_injection(text) -> list[str]`: the names of `INJECTION_PATTERNS` (given regexes) that
  match, case-insensitively, in the dict's order, then `"hidden_characters"` if the text contains a
  zero-width character (`​`, `‌`, `‍`, `⁠`, `﻿`), then
  `"markdown_image"` if it contains a Markdown image `![...](http...)`.
- `wrap_untrusted(source, text)`: returns

  ```text
  <untrusted source="SOURCE">
  TEXT
  </untrusted>
  ```

  where any `<untrusted` or `</untrusted` inside the text has its `<` replaced by `&lt;` (so the
  content cannot close the block), and `"` in the source is replaced by `&quot;`.

## 3. Tool policy

`ToolPolicy(rules)` maps tool names to `ToolRule(side_effect=False, owner_argument=None,
limits={})`. `decide(call, session) -> Decision(action, reason)` checks, in order:

1. no rule for the tool: `deny`, `"tool 'x' is not allowed"`;
2. `owner_argument` set and that argument differs from `session.user_id`: `deny`,
   `"not the session user's data"`;
3. a `limits` entry (argument name to maximum) exceeded: `deny`, `"amount 500 exceeds the limit
   100"` (argument, value, limit);
4. a side effect while `session.tainted` (untrusted content was read this turn): `confirm`,
   `"side effect after reading untrusted content"`;
5. any other side effect: `confirm`, `"side effect needs the user's confirmation"`;
6. otherwise `allow`, reason `""`.

## 4. Output check

`check_output(text, allowed_domains) -> list[str]`: for each Markdown image `![alt](url)` whose
host is not allowed, `"image from untrusted domain HOST"`; for each other `http://` or `https://`
URL whose host is not allowed, `"link to untrusted domain HOST"`. A host is allowed when it is an
allowed domain or a subdomain of one. Problems in order of appearance.

Run the visible tests with `uv run pytest`. There are 31 tests: 2 in `tests/test_visible.py`
and 29 hidden ones, plus ruff on each module.
