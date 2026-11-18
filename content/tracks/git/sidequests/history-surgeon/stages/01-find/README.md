First, find what has to go. Both commands look at **every version of every file ever
committed** on any branch or tag (`--branches --tags`), not only at `HEAD`: a file deleted a
year ago is still in every clone.

A good source is `git log --branches --tags --raw --no-abbrev --no-renames -m --format=`:
for each commit it lists the files it changed with their new blob ids, so together they are
every (blob, path) pair of history. `git cat-file --batch-check` gives the sizes of many
objects in one process.

**`scrub.sh big REPO [--min-size BYTES]`** lists the versions of files of at least `BYTES`
bytes (default `1048576`, 1 MiB):

```text
$ bash scrub.sh big ~/src/shop --min-size 500000
314572800	media/demo.mp4	history
2097152	vendor/libpay.a	HEAD
2000000	vendor/libpay.a	history
```

- Fields: size in bytes, path, and where it is: `HEAD` when that exact blob is at that path in
  `HEAD`'s tree, `history` otherwise (an older version, or a file deleted since). Separated by
  tabs.
- One line per (blob, path) pair: the same content committed under two names is two lines.
- Sorted by size (largest first), then path, then where, in byte order.

**`scrub.sh secrets REPO`** looks inside every blob for three kinds of secrets:

| kind | looks like |
|---|---|
| `aws-access-key` | `AKIA` followed by 16 uppercase letters or digits |
| `github-token` | `ghp_` followed by 36 letters or digits |
| `private-key` | a line `-----BEGIN ... PRIVATE KEY-----` (RSA, EC, OPENSSH, or none) |

```text
$ bash scrub.sh secrets ~/src/shop
aws-access-key	config/prod.env	history
private-key	deploy/id_ed25519	HEAD
```

- Fields: kind, path, where (as in `big`). One line per distinct line, sorted by path, then
  kind, then where.
- Exit status `1` when anything is found (so CI can fail on it), `0` when history is clean.

Both print nothing and exit `0` on a repository without commits. Errors: no command, an
unknown one, a wrong argument or a `--min-size` that is not a whole number prints
`usage: scrub.sh big|secrets|purge|redact|undo|expire REPO [ARGS]` on stderr and exits `2`; a
`REPO` that is not a Git repository prints `scrub.sh: not a git repository: REPO` and exits
`2`. Both commands only read: the repository must not change.
