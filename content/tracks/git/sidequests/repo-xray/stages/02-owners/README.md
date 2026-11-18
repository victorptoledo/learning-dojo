`gitx.sh owners REPO PATH` tells who wrote the lines that are in `PATH` today: for every text
file tracked at `HEAD` under `PATH` (a file or a folder, relative to the top of the repository;
`.` is the whole repository), `git blame` gives the author of each line. It prints the lines
per author, their share, the total and the **bus factor**:

```text
$ bash gitx.sh owners ~/src/shop src/billing
412	69%	Ana Souza
151	25%	Bruno Lima
33	6%	Carla Dias
total: 596 lines in 7 files
bus factor: 1
```

- One line per author: lines, share and name, separated by tabs, sorted by lines (most
  first), then by name in byte order. The share is `100 × lines / total`, rounded to the
  nearest whole number (halves round up).
- `total` counts the lines and the text files blamed. Binary files are skipped (they have no
  lines); an empty text file is a file with 0 lines.
- The **bus factor** is the smallest number of authors (taken from the top of the list) who
  together wrote **more than half** of the lines: the people the code depends on.
- Names go through `.mailmap`, as in `authors` (`git blame` already applies it).
- Blame the committed version (`HEAD`), not the working tree: uncommitted edits do not count.
- **Ignored revisions.** Formatting commits (a new indentation, a reformatter run) make one
  person "own" lines they never wrote. When `HEAD` has a `.git-blame-ignore-revs` file at the
  top of the repository (full hashes, one per line, `#` comments allowed), those commits are
  ignored: their lines go to whoever wrote them before (`git blame --ignore-revs-file`).
- File names may contain spaces.

Errors: `PATH` without any text file tracked at `HEAD` (or a repository without commits)
prints `gitx.sh: no files to blame under PATH` on stderr and exits `1`. A wrong number of
arguments is the usual usage error (`2`).
