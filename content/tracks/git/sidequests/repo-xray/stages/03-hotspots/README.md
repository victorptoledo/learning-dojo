`gitx.sh hotspots REPO [--since DATE] [--top N]` lists the files that change most: the
places where bugs and merge conflicts tend to live. For every file tracked at `HEAD` it counts
the commits that changed it and its churn (lines added plus lines removed), **following
renames**: a file's old commits count under its name of today.

```text
$ bash gitx.sh hotspots ~/src/shop --since 2024-01-01 --top 3
14	610	src/billing/invoice.py
9	122	src/cart.py
9	87	README.md
```

- Fields: commits, churn and the path at `HEAD`, separated by tabs. Sorted by commits (most
  first), then churn (most first), then path in byte order.
- Count the commits of `HEAD` without merges, as in `authors`; binary changes have no lines.
- **Renames.** Git detects them when you ask (`git log -M`, with `-z` so names are never
  quoted). Walk the history from newest to oldest and remember, for every old name, the name
  it has today: when a commit renamed `src/a.py` to `lib/b.py`, the older commits on
  `src/a.py` belong to `lib/b.py`. A rename is a change of the file (it counts as a commit).
  A name that comes back later is another file: the commits made after the rename on a new
  `src/a.py` stay with it.
- Files that are not at `HEAD` any more (deleted, or renamed away and then deleted) are not
  listed.
- `--since DATE` keeps the commits made since that date (`git log --since`, so
  `2024-01-01` or `3 months ago` both work). `--top N` prints at most `N` lines, `10` by
  default. The options may come in any order.
- A repository without commits prints nothing.

Errors: an unknown option, an option without its value or a `--top` that is not a whole
number greater than 0 is the usual usage error (`2`).
