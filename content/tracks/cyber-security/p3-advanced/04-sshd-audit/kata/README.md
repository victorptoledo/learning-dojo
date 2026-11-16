# SSH Config Audit

Write `audit.sh CONFIG`, which audits an OpenSSH server configuration file and prints its
findings. `sshd_config.example` is a file to try it on.

## Reading the file like sshd does

- Leading and trailing spaces, and a final `\r` (Windows line ends), are ignored; empty
  lines and lines starting with `#` are comments.
- A line is a **keyword** and a **value**, separated by spaces or tabs, or by `=` with optional
  spaces around it: `PermitRootLogin yes`, `PermitRootLogin=yes`, `PermitRootLogin = yes`.
- Keywords are **case-insensitive** (`permitrootlogin`); so are the `yes`/`no` values.
- For every keyword, the **first** value in the file wins; later lines for the same keyword are
  ignored.
- Only the **global** section is audited: stop reading at the first `Match` line.
- `Include` lines are not followed.

When a keyword is not set, its OpenSSH default applies: `PermitRootLogin prohibit-password`,
`PermitEmptyPasswords no`, `PasswordAuthentication yes`, `MaxAuthTries 6`,
`X11Forwarding no`. `Ciphers` and `MACs` are only checked when set.

## The checks, in this order

| Severity | Finding line | When |
| --- | --- | --- |
| `HIGH` | `HIGH PermitRootLogin yes: root may log in with a password` | the value is `yes` |
| `HIGH` | `HIGH PermitEmptyPasswords yes: accounts without a password may log in` | the value is `yes` |
| `MEDIUM` | `MEDIUM PasswordAuthentication VALUE: allow keys only` | the value is `yes` |
| `MEDIUM` | `MEDIUM MaxAuthTries VALUE: allow at most 4 attempts` | the value is not a whole number from 1 to 4 |
| `LOW` | `LOW X11Forwarding yes: disable unless needed` | the value is `yes` |
| `HIGH` | `HIGH Ciphers LIST: weak algorithms` | some entry contains `cbc`, `arcfour` or `3des` |
| `HIGH` | `HIGH MACs LIST: weak algorithms` | some entry contains `md5` or `sha1` followed by `-`, `@` or the end, or starts with `umac-64` |

- `VALUE` is the value as written in the file (case kept), or the default followed by
  ` (default)` when the keyword is not set: `MEDIUM PasswordAuthentication yes (default): allow keys only`.
- `LIST` is the **weak** entries only, in file order, joined by commas, case kept. A list
  value may start with `+` or `^` (add to or put before the defaults): ignore that first
  character. A list starting with `-` removes algorithms and is never a finding.

After the findings, always print `findings: N (high: H)`.

```text
$ bash audit.sh sshd_config.example
HIGH PermitRootLogin yes: root may log in with a password
MEDIUM PasswordAuthentication yes (default): allow keys only
MEDIUM MaxAuthTries 10: allow at most 4 attempts
HIGH Ciphers aes128-cbc: weak algorithms
findings: 4 (high: 2)
```

## Exit status and errors

`0` when there is no `HIGH` finding, `1` when there is at least one (so a CI job fails).
Without exactly one argument, print `usage: audit.sh CONFIG` on stderr and exit `2`; if
`CONFIG` is not a readable file, print `error: cannot read CONFIG` (with the given path) on
stderr and exit `2`.

Run the visible tests with `bats tests`. There are 12 tests: 2 in `tests/visible.bats` and 10
hidden ones, plus ShellCheck on `audit.sh`.
