# Guard the Server: a pre-receive Hook

Client-side hooks are a courtesy: anyone can skip them with `--no-verify` or never install
them. The rules a team really depends on run **on the server**, before a push is accepted.
Write `guard.sh`, installed as `hooks/pre-receive` of a bare repository. Git runs it once per
push, in the bare repository, with one line per updated ref on stdin:

```text
OLD-HASH NEW-HASH REFNAME
```

A zero hash (`0000…`) as `OLD` means the ref is being created; as `NEW`, deleted. While the
hook runs, the pushed objects are already readable but no ref has moved yet. If the hook
exits non-zero, **no** ref of the push is updated, and everything it wrote to stderr reaches
the pusher prefixed with `remote: `.

The rules:

| Ref | Rule | Message |
| --- | --- | --- |
| `refs/heads/main` | may not be deleted | `deleting main is not allowed` |
| `refs/heads/main` | only fast-forwards | `non-fast-forward push is not allowed` |
| `refs/heads/main` | linear history: no merge commit may **join** main, even one the server already has on another branch | `merge commit HASH is not allowed, rebase instead` |
| `refs/tags/v*` | a release tag is annotated | `release tags must be annotated` |
| `refs/tags/v*` | a release tag never moves and is never deleted | `release tags are immutable` |
| any ref | no **new** file bigger than `guard.maxBlobSize` bytes (server config, 1 MiB = `1048576` when unset) | `PATH is SIZE bytes (limit LIMIT), use Git LFS` |
| any ref | no **new** file containing an AWS access key id (`AKIA` and 16 capitals or digits) or a `-----BEGIN … PRIVATE KEY-----` line | `possible secret in PATH` |

Other branches and other tags are free: they may merge, be force-pushed and be deleted.

"New" means every object the push brings that the server does not have yet: **every version**
of a file in the pushed commits, not only the files at the tip (a key added and then removed
in the same push is still in history). Objects the server already has were checked when they
came in, so they are not checked again, even if the limit changed since.

Every message is one line on stderr, `guard: REF: MESSAGE`. Check every line of the push and
report **every** problem, then a last line `guard: push rejected (N problems)` and exit `1`. A
push without problems prints nothing and exits `0`.

```text
$ git push origin main
remote: guard: refs/heads/main: non-fast-forward push is not allowed
remote: guard: refs/heads/main: possible secret in config/aws.env
remote: guard: push rejected (2 problems)
To ../origin.git
 ! [remote rejected] main -> main (pre-receive hook declined)
```

Commands: `merge-base --is-ancestor`, `rev-list --min-parents=2`, `rev-list --objects`
with `--not --all`, `cat-file -t`, `cat-file --batch-check` with a format, `cat-file blob`,
`config --int`, and `grep -E`.

There are 16 tests: 1 in `tests/visible.bats` and 15 hidden ones, plus ShellCheck.
