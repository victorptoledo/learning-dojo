# Sidequest: Downloads organizer

Everyone's downloads folder is a mess. Build `organizer.py`, a command that puts every file
in a folder for its kind, safely.

The entry point is fixed: `main(argv: list[str]) -> int` in `organizer.py`, which returns the
exit code, prints results with `print` and problems to `sys.stderr`. The contract tests call
`main` with arguments on folders they create and check the files on disk and what was
printed. Everything else (a dict of categories, a `dataclass` for a move, `pathlib`,
helper functions) is yours.

```text
python organizer.py sort ~/Downloads
```

Use the standard library only. `tests/` is yours too: add your own pytest tests there.
