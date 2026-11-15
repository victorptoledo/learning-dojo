# Permission Audit

Two scripts, each taking a directory.

## `audit.sh DIR`

Prints one line per finding, as `PATH: KIND` where `PATH` is relative to `DIR`. Kinds:

| Kind | When |
| --- | --- |
| `world-writable` | a file or directory that **others** may write, except directories with the sticky bit (like `/tmp`) |
| `setuid` | a regular file with the setuid bit |
| `setgid` | a regular file with the setgid bit |

An entry can have several findings. Lines are sorted in byte order (`LC_ALL=C sort`).
`DIR` itself is not reported. With findings the script exits `1`; with none it prints
`no findings` and exits `0`.

```text
$ bash audit.sh tree
bin/tool: setuid
notes.txt: world-writable
shared: world-writable
$ echo $?
1
```

## `fix.sh DIR`

Resets every entry under `DIR` (and `DIR` itself):

- directories to `755`;
- regular files to `644`, except names ending in `.sh`, which get `755`;
- symbolic links are left alone.

It prints `fixed N`, where `N` is the number of entries whose mode **changed**
(`chmod -c` prints a line for each change).

## Both scripts

- no argument or more than one: `usage: audit.sh DIR` (or `fix.sh DIR`) on stderr, exit `2`;
- `DIR` is not a directory: `audit.sh: not a directory: DIR` (or `fix.sh: ...`) on stderr,
  exit `2`.

Tools: `find -perm -o+w`, `-perm -4000`, `-perm -2000`, `-type d`, `-type f`, `!`, `\( \)`,
`-printf '%P'`, `-exec chmod 755 {} +`, `chmod -c`, `wc -l`, `[ -d "$dir" ]`.

There are 10 tests: 1 in `tests/visible.bats` and 9 hidden ones, plus ShellCheck for each
script.
