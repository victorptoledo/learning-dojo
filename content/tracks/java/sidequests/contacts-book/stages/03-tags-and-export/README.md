Group contacts and take them elsewhere.

- `tag <email> <tag>` adds a tag and prints `tagged <name> with <tag>`. Tags are stored in
  lower case and may hold `a-z`, `0-9` and `-`; anything else is exit 1 with
  `invalid tag: <tag>`. An unknown email is exit 1 with `no contact with email <email>`.
  Tagging twice with the same tag changes nothing.
- `list` shows the tags of a contact after its line, sorted, in brackets:
  `Ana Lima <ana@example.com> [family, work]`. A contact without tags keeps the old format.
- `list --tag <tag>` lists only the contacts with that tag (same format; `no contacts` when
  none).
- `export` prints the whole book as CSV, in `list` order:

```text
name,email,phone,tags
Ana Lima,ana@example.com,+55 11 91234-5678,family;work
"Lima, Bia ""B""",bia@example.com,,
```

  The header comes first; tags are joined with `;`; an empty phone or tag list is an empty
  field. A field that holds a comma, a quote or a line break is wrapped in quotes, with every
  quote doubled (RFC 4180).
