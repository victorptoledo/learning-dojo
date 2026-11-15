# Streams, Redirection, Pipes and Exit Codes

The Unix idea: small programs that each do one thing, connected by **text streams**. Master
this and a few lines of shell replace a whole program.

## Three streams

Every process starts with three open **file descriptors**:

| fd | Name | Default |
| --- | --- | --- |
| 0 | **stdin**, standard input | the keyboard |
| 1 | **stdout**, standard output | the terminal |
| 2 | **stderr**, standard error | the terminal |

Results go to stdout, messages and errors to stderr. Keeping them apart is what lets you
pipe the results and still see the errors.

## Redirection

```bash
ls /etc > files.txt          # stdout to a file (created or TRUNCATED)
ls /etc >> files.txt         # append instead
sort < names.txt             # stdin from a file
find / -name passwd 2> errors.txt    # stderr to a file
find / -name passwd 2> /dev/null     # throw the errors away
make > build.log 2>&1        # stdout to the file, then stderr to where stdout goes
make &> build.log            # bash shorthand for the same
```

Redirections are applied **left to right**. `2>&1` means "make fd 2 a copy of fd 1 **as it
is now**". So `cmd 2>&1 > out.log` copies stderr to the terminal first, then moves stdout to
the file: the errors still reach the screen.

`/dev/null` discards everything written to it. A **here-string** feeds a text to stdin:
`tr a-z A-Z <<< "hello"`. A **here-document** feeds several lines:

```bash
cat > motd.txt <<EOF
Welcome to $(hostname)
EOF
```

## Pipes

`a | b` connects a's stdout to b's stdin. Both run **at the same time**: `b` reads as `a`
writes.

```bash
grep -c ERROR app.log                          # count matching lines
cut -d: -f1 /etc/passwd | sort | head -n 5     # first field, sorted, first five
ps aux | grep '[n]ginx'                        # processes (the [n] hides grep itself)
cat access.log | cut -d' ' -f1 | sort | uniq -c | sort -rn | head -n 3
```

The last line is the classic "top N": extract a field, **sort** so equal lines are next to
each other, `uniq -c` counts adjacent duplicates, `sort -rn` orders by count, `head` keeps the
top.

The tools you will combine most:

| Tool | Does | Example |
| --- | --- | --- |
| `grep PATTERN` | keep matching lines (`-v` the others, `-i` ignore case, `-c` count, `-E` extended regex) | `grep -E ' (4|5)[0-9]{2} '` |
| `cut -d' ' -f2,5` | select fields by a one-character delimiter | `cut -d, -f3` |
| `sort` | sort lines (`-n` numbers, `-r` reverse, `-k2,2` by field 2, `-u` unique, `-t,` delimiter) | `sort -k1,1nr -k2,2` |
| `uniq` | merge **adjacent** equal lines (`-c` count, `-d` only duplicates) | `uniq -c` |
| `wc -l` | count lines | `grep x f \| wc -l` |
| `head` / `tail` | first / last lines (`tail -n +2` skips the first line) | `tail -n +2 data.csv` |
| `tr` | translate or delete characters (`-d`, `-s` squeeze) | `tr -s ' '` |
| `tee file` | write to a file **and** pass stdout on | `make \| tee build.log` |
| `sed 's/a/b/'` | replace text in each line (Phase 2 goes deeper) | `sed 's/^ *//'` |

`uniq -c` pads the counts with spaces: `      5 /index.html`. Remove them with
`sed 's/^ *//'`.

## Exit codes

Every command ends with an **exit status**: `0` means success, anything else (1-255) failure.

```bash
grep -q root /etc/passwd
echo $?                       # 0: found; grep returns 1 for "no match", 2 for an error
mkdir build && cd build       # && runs the second only if the first succeeded
cd /nope || echo "no such directory" >&2   # || runs the second only if the first failed
```

> [!TIP]
> `grep -c` and `grep` return **1 when nothing matched**. In a script with error checking
> (Phase 2), a count of zero would stop it: write `grep -c ERROR app.log || true`.

`>&2` sends a message to stderr: use it for errors and usage text in your scripts. `exit 2`
ends a script with status 2 (by convention: wrong usage).

## Command substitution and arithmetic

`$(command)` runs a command and puts its stdout in place:

```bash
today=$(date +%F)
count=$(wc -l < access.log)          # < avoids wc printing the file name
echo "Lines on $today: $count"
total=$(( count * 2 + 1 ))           # integer arithmetic
```

## Summary

- stdin 0, stdout 1, stderr 2; results to stdout, messages to stderr (`>&2`).
- `>` truncates, `>>` appends, `2>` redirects errors, `2>&1` copies, left to right.
- Pipes connect small tools; `sort | uniq -c | sort -rn | head` is the top-N pattern.
- Exit status 0 is success; `&&`, `||` and `$?` act on it; grep returns 1 for no match.
- `$(...)` captures output and `$(( ))` does integer arithmetic.

## Further reading

- GNU Bash manual, "Redirections" and "Pipelines": <https://www.gnu.org/software/bash/manual/bash.html>
- BashGuide, "InputAndOutput": <https://mywiki.wooledge.org/BashGuide/InputAndOutput>
- GNU Coreutils manual (sort, uniq, cut, tr, wc): <https://www.gnu.org/software/coreutils/manual/coreutils.html>
- *The Linux Command Line*, chapters 6 and 20: <https://linuxcommand.org/tlcl.php>
