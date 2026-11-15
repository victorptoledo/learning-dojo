# Log Report

Write `logstats.sh FILE`. `FILE` is an access log in the common log format, one request per
line, fields separated by single spaces:

```text
10.0.0.1 - - [14/Nov/2026:10:00:01 +0000] "GET /index.html HTTP/1.1" 200 512
^ IP (field 1)                             ^ path (field 7)          ^ status (field 9)
```

It prints exactly these lines (here for `sample.log`, which is in the kata folder):

```text
requests: 8
visitors: 4
errors: 3
top paths:
3 /index.html
2 /about.html
1 /api/orders
```

| Line | Meaning |
| --- | --- |
| `requests` | number of lines |
| `visitors` | number of **different** IP addresses |
| `errors` | requests whose status starts with 4 or 5 |
| `top paths` | the 3 most requested paths as `COUNT PATH`, most requested first; equal counts in alphabetical order of the path; fewer lines if there are fewer paths |

An empty log prints `requests: 0`, `visitors: 0`, `errors: 0` and `top paths:`.

Errors:

- no argument or more than one: print `usage: logstats.sh FILE` to **stderr**, exit `2`;
- a file that does not exist or cannot be read: print `logstats.sh: cannot read FILE` to
  stderr (with the name given), exit `1`.

Tools: `wc -l < file`, `cut -d' ' -fN`, `sort`, `sort -u`, `uniq -c`, `sort -k1,1nr -k2,2`,
`head -n 3`, `grep -c`, `sed 's/^ *//'`, `[ -r "$file" ]`, `$#`.

There are 9 tests: 1 in `tests/visible.bats` and 8 hidden ones, plus ShellCheck.
