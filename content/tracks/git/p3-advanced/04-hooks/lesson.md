# Git Hooks: Client-side and Server-side Automation

A **hook** is an executable that Git runs at a fixed moment, such as just before a commit or
just before a push. Its **exit code** decides: for the "pre" hooks, non-zero stops the
operation. Hooks live in `.git/hooks/` (with `.sample` examples) and are **not** part of the
repository: cloning does not copy them.

## The useful client-side hooks

| Hook | Runs | Arguments / input | Typical use |
| --- | --- | --- | --- |
| `pre-commit` | before the message is asked for | none (inspect the index) | lint and format the staged files, block large files or secrets |
| `prepare-commit-msg` | before the editor opens | message file, source | add the ticket number from the branch name |
| `commit-msg` | after the message is written | path of the message file | enforce the message format |
| `post-commit` | after the commit | none | notifications (cannot block) |
| `pre-push` | before objects are sent | remote name and URL; refs on stdin | run the fast tests, block pushes to `main` |
| `post-checkout`, `post-merge` | after switch/merge | old and new refs | reinstall dependencies when the lock file changed |
| `pre-rebase` | before a rebase | upstream, branch | refuse to rebase published branches |

A minimal `commit-msg` hook:

```bash
#!/usr/bin/env bash
# .git/hooks/commit-msg - $1 is the file holding the proposed message.
subject=$(grep -v '^#' "$1" | head -n 1)
if ! [[ $subject =~ ^(feat|fix|docs|refactor|test|chore)(\([a-z0-9-]+\))?!?:\ .+ ]]; then
    echo "commit-msg: use 'type(scope): description'" >&2
    exit 1
fi
```

Make it executable (`chmod +x`). Hooks run with the repository root as working directory
(for most hooks), receive Git's environment (`GIT_DIR`, `GIT_INDEX_FILE` in some cases), and
their stderr is shown to the user.

## Sharing hooks with a team

Because `.git/hooks` is not versioned, teams keep hooks in the repository and point Git at
them:

```bash
git config core.hooksPath .githooks     # every hook is now read from .githooks/
git rev-parse --git-path hooks          # where Git looks for hooks right now
```

Or use a framework that installs and runs them:

| Tool | Ecosystem | Config |
| --- | --- | --- |
| pre-commit | any (written in Python) | `.pre-commit-config.yaml` |
| Husky + lint-staged | Node | `.husky/`, `package.json` |
| lefthook | any (Go binary) | `lefthook.yml` |

They still need one install step per clone (`pre-commit install`, `npm install` runs Husky's
`prepare` script).

## Keep them fast, and know their limits

- A slow hook gets disabled. Check only **staged** files, keep the full test suite for CI.
- `git commit --no-verify` and `git push --no-verify` skip `pre-commit`, `commit-msg` and
  `pre-push`. A clone without the install step has no hooks at all.

So client-side hooks are a **convenience** that catches mistakes early. Rules that matter
must also be enforced where nobody can skip them.

## Server-side hooks and their hosted equivalents

On a self-hosted Git server, `pre-receive` and `update` run when a push arrives and can
reject it; `post-receive` triggers deployments or notifications. Hosting services do not let
you install arbitrary hooks; they give you the same powers as features:

| Need | Hosted equivalent |
| --- | --- |
| reject pushes to `main` | protected branches / rulesets |
| require passing tests | required status checks (CI) |
| block secrets | push protection (secret scanning) |
| enforce message or branch name format | rulesets with metadata restrictions, or a CI check |
| run something after a push | webhooks, CI triggers |

## Summary

- Hooks are executables in `.git/hooks` (or `core.hooksPath`) that Git runs at fixed
  points; a non-zero exit from a `pre-*` or `commit-msg` hook aborts the operation.
- `commit-msg` gets the message file, `pre-push` the refs being pushed on stdin.
- Share hooks with `core.hooksPath` or a framework; they still need an install step.
- Client hooks can be skipped; enforce important rules on the server and in CI.

## Further reading

- Pro Git, "Git Hooks": <https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks>
- githooks reference: <https://git-scm.com/docs/githooks>
- Atlassian, "Git hooks": <https://www.atlassian.com/git/tutorials/git-hooks>
- The pre-commit framework: <https://pre-commit.com/>
- GitHub Docs, "About protected branches": <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches>
- roadmap.sh topic "Client vs Server Hooks": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/client-vs-server-hooks@zsU6R2zvJKnYNU2ac4o4p.md>
