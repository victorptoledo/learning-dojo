`gitx.sh authors REPO` prints one line per author of the commits reachable from `HEAD` in
the repository `REPO`: the number of commits, the lines added and the lines removed, and the
name. Fields are separated by one tab.

```text
$ bash gitx.sh authors ~/src/shop
12	+840	-133	Ana Souza
3	+51	-9	Bruno Lima
3	+12	-40	Carla Dias
```

- Lines are sorted by commits (most first), then by name in byte order.
- **Merge commits are not counted**, neither as commits nor as lines: their changes were
  already counted in the commits they merge.
- Lines come from `--numstat`. A binary file has no lines (`-` in numstat): it counts as 0.
- Names come through the repository's **`.mailmap`**: `Ana <ana@home.example>` and
  `ana.souza <ana@work.example>` mapped to `Ana Souza` are one author. Use `%aN`, the
  mailmap-aware name, not `%an`.
- Only the commits of the current branch (`HEAD`) count, not other branches.
- A repository without commits prints nothing and exits `0`.

Errors, Unix style: no command, an unknown command or a wrong number of arguments prints
`usage: gitx.sh authors|owners|hotspots REPO [ARGS]` on stderr and exits `2`; a `REPO` that
is not a Git repository prints `gitx.sh: not a git repository: REPO` on stderr and exits `2`.
