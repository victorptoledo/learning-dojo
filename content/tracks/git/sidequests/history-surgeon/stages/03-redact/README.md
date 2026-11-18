Purging a whole file is too much when the secret is one line of `settings.py`. `scrub.sh
redact REPO FILE` replaces strings **inside** every version of every file instead, like
`git filter-repo --replace-text` and BFG.

```text
$ cat leaked.txt
AKIAIOSFODNN7EXAMPLE
hunter2!
$ bash scrub.sh redact ~/src/shop leaked.txt
redacted 4 file versions
rewrote 37 commits, dropped 0
```

- `FILE` holds the strings to remove, one per line (a final `\r` is ignored, empty lines are
  skipped). They are **literal** text: `p@ss.w0rd/[x]*` means those characters, not a
  pattern.
- Every occurrence of every string, in every version of every file on every branch and tag,
  becomes `***REMOVED***`. Nothing else in the file changes: bytes around it, binary
  content, a missing final newline all stay as they were.
- **Only what held a secret changes.** A version of a file without any of the strings keeps
  its blob id; a commit whose tree does not change, and whose parents did not change, keeps
  its id. The rewrite keeps authors, dates, messages, merges and tags, as `purge` does.
  Commit messages are not touched.
- No commit is dropped by `redact`, even when two versions of a file become equal.
- The first line counts the versions of files (distinct blobs) that changed; the second is
  the one of `purge`. When no file holds any of the strings, it prints
  `redacted 0 file versions` and `rewrote 0 commits, dropped 0` and changes nothing.
- The working tree follows the new `HEAD`, as with `purge`.

**How.** Each blob that holds a string gets a redacted copy (`git hash-object -w`), giving a
map from old blob ids to new ones. In the `--no-data` stream of `fast-export`, the `M` lines
name blobs by id, so swapping ids there rewrites the history without copying any file
content. Keep `sed` away from the replacement: the strings are literal and may hold `/`, `.`
or `&`.

Refuse like `purge` (uncommitted changes, no commits). A `FILE` that cannot be read or holds
no strings prints `scrub.sh: no strings in FILE` and exits `1`; no `FILE` or more arguments
is the usage error (`2`).
