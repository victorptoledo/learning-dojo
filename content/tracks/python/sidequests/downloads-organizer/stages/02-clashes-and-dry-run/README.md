Never lose a file, and let the user look before leaping.

- When the category folder already has a file with that name, the moved file gets the first
  free name among `name (1).ext`, `name (2).ext`... (`report.pdf` → `report (1).pdf`; a file
  without extension → `notes (1)`). The line printed shows the new name:
  `moved report.pdf -> documents/report (1).pdf`.
- `main(["sort", folder, "--dry-run"])` prints the same lines starting with `would move`
  instead of `moved`, and changes nothing on disk (no folder is created either).
- Errors return a code and print one line on stderr:

| Situation | Code | stderr starts with |
|---|---|---|
| no command, unknown command or option, missing folder argument | 2 | `usage: organizer` |
| the folder does not exist or is a file | 1 | `error: not a folder: <folder>` |
