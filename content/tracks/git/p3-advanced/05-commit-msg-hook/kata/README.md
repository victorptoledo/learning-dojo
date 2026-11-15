# Commit Message Guard

Two scripts.

## `commit-msg.sh MESSAGE-FILE`

The hook. Git calls it with the path of the file holding the proposed message. It exits `0`
to accept the message, or prints `commit-msg: REASON` to **stderr** and exits `1`.

First, clean the message the way Git does: drop every line that starts with `#`, then drop
blank lines at the start. The first remaining line is the **subject**.

| Check, in this order | Reason when it fails |
| --- | --- |
| something is left after cleaning | `empty message` |
| subjects starting with `Merge `, `Revert "`, `fixup! `, `squash! ` or `amend! ` are accepted without further checks | |
| the subject is `type(scope)!: description`: `type` is one of `feat fix docs style refactor perf test build ci chore revert`; `(scope)` is optional, lower-case letters, digits and `-`; `!` is optional; then `: ` and a description that does not start with a space | `subject must look like 'type(scope): description'` |
| the subject has at most 72 characters | `subject is longer than 72 characters` |
| if there is a line after the subject, it is empty | `leave the second line empty` |

```text
feat(cart): add coupon codes          accepted
fix!: drop the v1 checkout API        accepted
Added coupons                         subject must look like 'type(scope): description'
```

## `install.sh REPO`

Installs `commit-msg.sh` (from the same folder as `install.sh`) as the `commit-msg` hook of
`REPO`, executable, in the directory where Git looks for hooks: `.git/hooks` normally, or
the one `core.hooksPath` names (`git rev-parse --git-path hooks` tells you). Create the
directory if it is missing.

If a different `commit-msg` hook is already there, rename it to `commit-msg.bak` first and
print `saved the previous hook as commit-msg.bak`. Then print `installed commit-msg hook`.

Errors: no argument or more than one: `usage: install.sh REPO` on stderr, exit `2`; not a
repository: `install.sh: not a git repository: REPO`, exit `1`.

Commands: `grep -v`, `mapfile -t`, `[[ =~ ]]`, `${#var}`, `git rev-parse --git-path hooks`,
`cmp -s`, `cp`, `chmod +x`, `mv`.

There are 13 tests: 1 in `tests/visible.bats` and 12 hidden ones, plus ShellCheck on both
scripts.
