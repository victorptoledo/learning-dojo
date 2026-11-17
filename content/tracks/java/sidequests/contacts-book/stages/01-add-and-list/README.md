Two commands, exit code 0:

- `--file <path> add <name> <email> [<phone>]` saves a contact and prints `added <name>`.
  The phone is optional.
- `--file <path> list` prints one line per contact, sorted by name ignoring case:

```text
Ana Lima <ana@example.com> +55 11 91234-5678
bruno <bruno@example.com>
```

  The phone comes after the email with one space, and is left out when there is none. An empty
  book prints `no contacts`.

The data lives in the file: an `add` in one run is listed by a `list` in the next run. Names
are stored as given (spaces included).
