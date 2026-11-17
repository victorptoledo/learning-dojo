Nobody wants `node_modules` in their backup. A file `.backupignore` at the top of `<src>`
lists what to leave out, in a subset of the `.gitignore` language:

```text
# logs, except one
*.log
!keep.log
build/
/secret.txt
docs/*.tmp
```

- Blank lines and lines starting with `#` mean nothing.
- A pattern without `/` matches the **name** of a file or folder at any depth (`*.log`).
- A pattern ending in `/` matches folders only, with everything inside them (`build/`).
- A pattern with a `/` anywhere else is relative to the top of `<src>`, and a leading `/` is
  dropped (`/secret.txt`, `docs/*.tmp`). Patterns use `path.Match` syntax: `*` never crosses
  a `/`.
- `!` before a pattern brings back what an earlier pattern left out. The **last** pattern that
  matches decides. As in git, a file inside a folder that is left out cannot be brought back.

Ignored paths are not copied, not removed by `--prune`, and not reported by `verify`. The
`.backupignore` file itself is backed up.
