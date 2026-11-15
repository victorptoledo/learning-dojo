# Bash Scripting: Variables, Quoting, Control Flow and Functions

A script is a file of commands with a **shebang** line that says which interpreter runs it:

```bash
#!/usr/bin/env bash
# backup-logs.sh: copies today's logs to /srv/backup
set -euo pipefail
```

Run it with `bash backup-logs.sh`, or make it executable (`chmod +x`) and run
`./backup-logs.sh`. Use `bash`, not `sh`, when you use bash features (arrays, `[[ ]]`):
on Debian `/bin/sh` is **dash**, a smaller POSIX shell.

## Variables and quoting

```bash
name="Ada Lovelace"       # no spaces around =
echo "Hello, $name"       # double quotes: variables expand, the result stays ONE word
echo 'Hello, $name'       # single quotes: nothing expands
echo "Files: ${count}x"   # braces separate the name from the text after it
```

**Quote every expansion: `"$var"`, `"$@"`, `"$(cmd)"`.** Unquoted, bash splits the value on
spaces and expands globs in it. That is the most common bash bug:

```bash
file="my report.txt"
rm $file       # runs: rm my report.txt   -> two files, neither exists
rm "$file"     # runs: rm 'my report.txt'
```

Arguments: `$1`... `$9`, `${10}`; `$#` how many; `"$@"` all of them, **each kept as its own
word** (use it to pass arguments on); `$0` the script name; `$?` the last exit status; `$$`
the shell's process id.

## Parameter expansion

| Expression | Gives |
| --- | --- |
| `${var:-default}` | `default` when var is unset or empty |
| `${var:?message}` | stops the script with `message` when var is unset or empty |
| `${#var}` | the length |
| `${file%.txt}` | remove the shortest match of `.txt` from the end: `report` |
| `${path##*/}` | remove the longest match of `*/` from the start: the file name |
| `${path%/*}` | the directory part |
| `${var/old/new}` | replace the first `old` (`//` replaces all) |
| `${var^^}` / `${var,,}` | upper / lower case |

## Tests and conditionals

```bash
if [[ -f "$config" && -r "$config" ]]; then
    echo "using $config"
elif [[ -z "${CONFIG_URL:-}" ]]; then
    echo "no configuration" >&2
    exit 1
else
    fetch_config
fi
```

| Test | True when |
| --- | --- |
| `-e` / `-f` / `-d` / `-r` / `-x` | exists / regular file / directory / readable / executable |
| `-z "$s"` / `-n "$s"` | empty / not empty |
| `"$a" == "$b"` / `!=` | strings equal / differ (`[[ ]]` allows a glob on the right: `[[ $f == *.log ]]`) |
| `$s =~ ^[0-9]+$` | matches a regular expression (in `[[ ]]`) |
| `-eq -ne -lt -le -gt -ge` | integer comparisons; or `(( n > 5 ))` |

`[[ ]]` is bash's test: safer with empty values and it knows patterns. `[ ]` is the
portable POSIX `test` command, where every variable must be quoted.

`case` matches one value against patterns:

```bash
case "$1" in
    start|stop) systemctl "$1" app ;;
    *.tar.gz)   tar -xzf "$1" ;;
    *)          echo "usage: $0 start|stop|ARCHIVE" >&2; exit 2 ;;
esac
```

## Loops

```bash
for host in web1 web2 db1; do ssh "$host" uptime; done
for file in /var/log/*.log; do        # a glob, never $(ls)
    [[ -e "$file" ]] || continue      # no match leaves the pattern itself
    gzip "$file"
done
for (( i = 1; i <= 3; i++ )); do echo "try $i"; done

while IFS= read -r line; do           # read a file line by line, exactly
    echo "got: $line"
done < input.txt
```

`IFS=` keeps leading spaces and `-r` keeps backslashes. To read a command's output, use
`done < <(command)`: a pipe (`command | while ...`) runs the loop in a subshell, so variables
set inside are lost after it.

## Functions and arrays

```bash
log() {
    local level="$1"; shift           # local: not visible outside the function
    printf '%s [%s] %s\n' "$(date +%T)" "$level" "$*" >&2
}

is_port() {
    [[ "$1" =~ ^[0-9]+$ ]] && (( $1 >= 1 && $1 <= 65535 ))   # the status is the result
}

servers=(web1 web2 "db 1")            # an array
servers+=(cache1)
echo "${#servers[@]} servers, first ${servers[0]}"
for s in "${servers[@]}"; do echo "$s"; done
```

A function returns a **status** (`return 1`), and prints its **result** to stdout:
`name=$(get_name)`. `printf` is more predictable than `echo` for formatted output.

## Options with getopts

```bash
verbose=false; keep=5
while getopts ':vk:' opt; do
    case "$opt" in
        v) verbose=true ;;
        k) keep="$OPTARG" ;;
        :) echo "option -$OPTARG needs a value" >&2; exit 2 ;;
        *) echo "usage: $0 [-v] [-k KEEP] DIR" >&2; exit 2 ;;
    esac
done
shift $(( OPTIND - 1 ))              # what is left are the operands
```

A letter followed by `:` takes a value (`-k 3`); the leading `:` lets you report errors
yourself.

## Strict mode and cleanup

```bash
set -euo pipefail
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT             # runs whenever the script exits
```

- `-e`: stop when a command fails. It does **not** apply inside `if`, `while`, `&&` or `||`
  tests, and a command that may fail on purpose needs `|| true`.
- `-u`: an unset variable is an error (use `${var:-}` when unset is fine).
- `-o pipefail`: a pipeline fails if **any** stage fails, not only the last.

They catch many mistakes, but `-e` has surprising corner cases; still check important steps
explicitly. `trap ... EXIT` with `mktemp` is the reliable way to clean up temporary files.

Finally, run **ShellCheck** on every script: it knows all of the pitfalls above.

## Summary

- Shebang `#!/usr/bin/env bash`; on Debian `sh` is dash.
- Quote every expansion; `"$@"` passes the arguments on unchanged.
- `${var:-default}`, `${file%.ext}`, `${path##*/}` edit strings without extra programs.
- `[[ ]]` for tests, `case` for patterns, loops over globs, `while IFS= read -r`.
- `local` in functions, arrays with `"${a[@]}"`, `getopts` for options.
- `set -euo pipefail` plus `trap ... EXIT`, and ShellCheck.

## Further reading

- BashGuide: <https://mywiki.wooledge.org/BashGuide> and BashPitfalls: <https://mywiki.wooledge.org/BashPitfalls>
- BashFAQ (reading lines, getopts, error handling): <https://mywiki.wooledge.org/BashFAQ>
- GNU Bash manual: <https://www.gnu.org/software/bash/manual/bash.html>
- ShellCheck wiki, one page per warning: <https://www.shellcheck.net/wiki/>
