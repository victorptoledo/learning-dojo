Bad input gets a clear message and an exit code; nothing is saved when a command fails.

| Situation | Exit code | `err` |
|---|---|---|
| missing or unknown command, missing arguments | 2 | a line starting with `usage: contacts` |
| an email that is not `something@domain.tld` (no spaces, one `@`, a dot after it) | 1 | `invalid email: <email>` |
| an email already in the book, ignoring case | 1 | `duplicate email: <email>` |

New commands:

- `find <text>` prints the contacts whose name **or** email contains `<text>`, ignoring case,
  in the `list` format and order. Exit 0 when one matched; 1 (and nothing printed) otherwise.
- `remove <email>` (email ignoring case) prints `removed <name>`, or fails with exit 1 and
  `no contact with email <email>`.

Names may hold any character but a line break, commas, quotes and semicolons included:
`Lima, Ana "Nana"` must come back exactly from the file.
