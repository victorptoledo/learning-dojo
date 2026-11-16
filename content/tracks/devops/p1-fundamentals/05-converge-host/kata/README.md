# Converge a Host: Idempotent Configuration in Bash

Configuration management tools (Ansible, Puppet, Chef, Salt) all do the same thing: read a
**desired state**, compare it with the machine, and change only what differs. Write the core
of one in bash: `converge.sh [--check] ROOT`.

`ROOT` is the folder that plays the server's `/`, so the tests can run it on throwaway
trees. The desired state is `desired.txt` next to the script, one resource per line
(blank lines and lines starting with `#` are skipped; fields are separated by spaces):

| Kind | Line | Desired state |
| --- | --- | --- |
| `dir` | `dir PATH MODE` | `ROOT/PATH` is a directory with that mode (`0750` means `stat -c %a` prints `750`) |
| `file` | `file PATH TEMPLATE MODE` | `ROOT/PATH` holds the template rendered with `vars.env`, with that mode |
| `line` | `line PATH TEXT...` | `ROOT/PATH` contains the line `TEXT` exactly once; other lines are kept |
| `absent` | `absent PATH` | `ROOT/PATH` does not exist |

Rendering replaces every `${KEY}` in the template (a path relative to the script) with the
value of `KEY=VALUE` in `vars.env`.

For each resource, in file order, print one line, then a summary:

```text
ok: dir /etc/app
changed: file /etc/app/app.conf
changed: line /etc/hosts
changed: absent /etc/app/legacy.conf
summary: 3 changed, 1 ok
```

- A resource that already matches is `ok` and is **not touched** (no rewrite, no `chmod`):
  a second run prints only `ok` lines and `summary: 0 changed, 4 ok`.
- `--check` is a dry run: it writes nothing, prints `would change: ...` instead of
  `changed: ...` and `summary: N to change, M ok`, and exits **3** when anything would
  change (0 otherwise), so a CI job can detect drift.
- An unknown kind stops the run: `converge.sh: line N: unknown resource 'KIND'` on stderr
  (N counts every line of `desired.txt`, comments included), exit 1.
- No `ROOT`, or more than one argument: `usage: converge.sh [--check] ROOT` on stderr,
  exit 2.

Tools: `stat -c %a`, `chmod`, `mkdir -p`, `grep -qxF`, `printf '%s\n' >> file`, `rm -rf --`,
`while read -r kind path rest`, `case`, `${content//"\${$key}"/$value}`,
`here=$(cd "$(dirname "$0")" && pwd)`.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus ShellCheck.
