# Brute-Force Detector

Write `bruteforce.sh LOG [THRESHOLD]`, which reads an OpenSSH authentication log (the format
of `/var/log/auth.log` on Debian; `sample.log` is an example) and reports brute-force
sources. `THRESHOLD` defaults to `5`.

## The lines that count

Only lines from `sshd` count: they contain ` sshd[<pid>]: `. Every other line is ignored.
Lines may end with Windows line ends (`\r\n`): ignore a final `\r`.

```text
Nov 14 10:00:01 web1 sshd[811]: Failed password for root from 203.0.113.7 port 50122 ssh2
Nov 14 10:00:04 web1 sshd[811]: Failed password for invalid user admin from 203.0.113.7 port 50130 ssh2
Nov 14 10:07:12 web1 sshd[902]: Accepted password for deploy from 203.0.113.7 port 50410 ssh2
Nov 14 10:09:30 web1 sshd[940]: Accepted publickey for ada from 198.51.100.20 port 61000 ssh2: ED25519 SHA256:abc
```

- A **failure** is `Failed password for USER from IP port N ssh2` at the end of the line,
  where `USER` may be preceded by `invalid user `.
- A **success** is `Accepted METHOD for USER from IP port N ssh2`, optionally followed by
  `: ` and key details; `METHOD` is `password`, `publickey` or `keyboard-interactive/pam`.
- The username is chosen by whoever connects, and **may contain spaces and even the word
  `from`**. Take the IP and port from the **end** of the line (the last
  ` from IP port N ssh2`), and the username is everything between `for ` (after the optional
  `invalid user `) and that last ` from `. IPs are IPv4 or IPv6.

## The report

First one line per **suspect** source, an IP with at least `THRESHOLD` failures in the
whole log, ordered by failures (most first) and then by IP (as text):

```text
SUSPECT 203.0.113.7 failures=6 users=admin,oracle,root
```

`users` lists the distinct usernames tried, sorted, joined by commas.

Then one line per **successful login from an IP that already had at least `THRESHOLD`
failures before that line**, in log order, where `N` is that count:

```text
ALERT 203.0.113.7 accepted user=deploy after 6 failures
```

## Exit status and errors

| Situation | stdout | stderr | Status |
| --- | --- | --- | --- |
| No ALERT lines (SUSPECT lines or nothing) | the report | nothing | `0` |
| At least one ALERT | the report | nothing | `1` |
| No argument, or more than two | nothing | `usage: bruteforce.sh LOG [THRESHOLD]` | `2` |
| LOG is not a readable file | nothing | `error: cannot read LOG` (with the given path) | `2` |
| THRESHOLD is not a positive integer | nothing | `error: threshold must be a positive integer` | `2` |

Run the visible tests with `bats tests`. There are 13 tests: 2 in `tests/visible.bats` and 11
hidden ones, plus ShellCheck on `bruteforce.sh`.

> [!NOTE]
> All data here is synthetic and the addresses are from the documentation ranges
> (RFC 5737, RFC 3849). The kata detects attacks; it never makes any connection.
