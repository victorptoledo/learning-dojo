# Branch Report

Write `branches.sh REPO [BASE]`. `BASE` is a local branch, `main` when it is not given. For
every **other** local branch, in the order `git for-each-ref` lists them (by name, slashes
included), print one line:

```text
NAME ahead A behind B STATE
```

- `A`: commits on `NAME` that are not on `BASE`;
- `B`: commits on `BASE` that are not on `NAME`;
- `STATE`: `merged` when `A` is 0 (everything on the branch is already in `BASE`), otherwise
  `open`.

Then one last line: `total: N branches, M merged`.

For a repository like this one:

```text
main:            Initial - Add cart - (merge of fix/vat) - Add tax rules
fix/vat:         Initial - Fix VAT                (merged into main)
feature/search:  ... merge of fix/vat - Index products - Search box
docs:            points at the same commit as main
```

it prints:

```text
docs ahead 0 behind 0 merged
feature/search ahead 2 behind 1 open
fix/vat ahead 0 behind 3 merged
total: 3 branches, 2 merged
```

The script only reads: it must not switch branches or change any file or ref. A tag with the
same name as a branch must not confuse it.

Errors (message on **stderr**):

- no argument or more than two: `usage: branches.sh REPO [BASE]`, exit `2`;
- `REPO` is not a repository: `branches.sh: not a git repository: REPO`, exit `1`;
- `BASE` is not a local branch: `branches.sh: no branch BASE`, exit `1`.

Commands: `git for-each-ref --format='%(refname:lstrip=2)' refs/heads`,
`git rev-list --left-right --count A...B`, `git show-ref --verify -q`, `read -r`.

There are 10 tests: 1 in `tests/visible.bats` and 9 hidden ones, plus ShellCheck.
