# Text Processing: grep, sed, awk and Friends

Logs, CSV exports and config files are text. Three tools cover almost every need: **grep**
finds lines, **sed** edits them, **awk** computes over their fields.

## Regular expressions

| Pattern | Matches |
| --- | --- |
| `.` | any one character |
| `*` | the previous item 0 or more times |
| `^` / `$` | start / end of the line |
| `[abc]`, `[0-9]`, `[^,]` | one character from the set / not from it |
| `[[:digit:]]`, `[[:space:]]`, `[[:alpha:]]` | character classes, safe in any locale |
| `+`, `?`, `{2,4}` | 1 or more, 0 or 1, 2 to 4 times (**extended** syntax) |
| `a\|b`, `( )` | alternatives and groups (extended syntax) |
| `\b`, `\w` | word boundary, word character (GNU) |

**Basic** regular expressions (the default of grep and sed) need backslashes for `+ ? | ( ) {}`.
Use the **extended** syntax with `grep -E` and `sed -E` and forget the difference.

## grep: find lines

```bash
grep -n 'timeout' app.log                  # with line numbers
grep -i 'error' app.log                    # ignore case
grep -v '^#' sshd_config | grep -v '^$'    # without comments and blank lines
grep -E ' (5[0-9]{2}) ' access.log         # 5xx statuses
grep -o -E '[0-9]+ms' app.log              # print only the matching part
grep -r --include='*.yaml' 'image:' k8s/   # search a tree
grep -C 2 'panic' app.log                  # 2 lines of context around each match
grep -F '1.2.3.4' access.log               # fixed string: dots are not "any character"
```

## sed: edit a stream

sed reads line by line, applies commands and prints the result.

```bash
sed 's/foo/bar/' file           # replace the first foo of each line
sed 's/foo/bar/g' file          # every foo
sed -E 's/([0-9]+)ms/\1 ms/g'   # \1 is the first group
sed 's|/usr/local|/opt|g'       # any delimiter: handy for paths
sed -n '10,20p' file            # print lines 10 to 20 only (-n: no automatic print)
sed '/^#/d' file                # delete comment lines
sed -n '/BEGIN/,/END/p' file    # print from a BEGIN line to an END line
sed -i.bak 's/debug=true/debug=false/' app.conf   # edit IN PLACE, keep app.conf.bak
```

> [!WARNING]
> `sed -i` rewrites the file. Run the command without `-i` first and read the output, or
> keep a backup with `-i.bak`.

## awk: fields and calculations

awk splits each line into fields `$1`, `$2`, ... (`$0` is the whole line, `NF` the number of
fields, `NR` the line number) and runs `pattern { action }` rules on every line:

```bash
awk '{ print $1 }' access.log                       # first field (split on spaces)
awk -F: '$3 >= 1000 { print $1 }' /etc/passwd       # -F sets the separator
awk '$9 >= 500' access.log                          # a pattern alone prints the line
awk '{ bytes += $10 } END { print bytes }' access.log
awk -F, 'NR > 1 { total[$2] += $4 } END { for (c in total) printf "%s %.2f\n", c, total[c] }' orders.csv | sort
```

The last example is the awk idiom: an **associative array** keyed by a field, filled line
by line, printed in `END`. `for (k in a)` has no defined order, so pipe into `sort`.

Useful pieces: `BEGIN { FS = ","; OFS = "," }` set the separators before the first line;
`printf "%-10s %8.2f\n", name, value` formats; `-v limit=90` passes a shell value in (never
paste shell variables into the awk program text); `next` skips to the next line;
`length($0)`, `tolower()`, `split()`, `sub()`, `gsub()` are built in.

```bash
threshold=90
df -P | awk -v t="$threshold" 'NR > 1 { sub(/%/, "", $5); if ($5 + 0 >= t) print $6, $5 "%" }'
```

## The other helpers

| Tool | Does |
| --- | --- |
| `cut -d, -f2` | one field by a single-character delimiter |
| `paste -d, a b` | join files side by side |
| `join -t, a b` | join two **sorted** files on a common field (like SQL) |
| `column -t -s,` | align columns for reading |
| `expand` / `unexpand` | tabs to spaces / spaces to tabs |
| `nl` | number lines |
| `split -l 1000 big.csv part-` | cut a file into pieces |
| `jq` | the same ideas for JSON (`jq -r '.items[].name'`) |

When should you stop and write a program? When the data has quoted fields with commas in
them (real CSV), nested structure, or the awk program does not fit on a screen.

## Summary

- Use extended regexes (`grep -E`, `sed -E`); `-F` for fixed strings.
- `grep -v`, `-o`, `-r`, `-C` cover most searches.
- `sed 's/a/b/g'`, `sed -n 'Np'`, `sed '/re/d'`, and `-i.bak` to edit in place.
- `awk -F, 'NR > 1 { sum[$1] += $3 } END { ... }'` groups and totals; pass values with `-v`.
- `cut`, `paste`, `join`, `column` and `jq` round out the toolbox.

## Further reading

- GNU Grep manual: <https://www.gnu.org/software/grep/manual/grep.html>
- GNU sed manual: <https://www.gnu.org/software/sed/manual/sed.html>
- The GNU Awk User's Guide: <https://www.gnu.org/software/gawk/manual/gawk.html>
- *The Linux Command Line*, chapters 19 and 20: <https://linuxcommand.org/tlcl.php>
