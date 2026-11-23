A CI pipeline should call one script that you can also run on your laptop. The toolbox
already has two tools (`bin/slugify.sh`, `bin/trim.sh`) and their bats tests (`test/`).
Write `scripts/ci.sh`:

```text
bash scripts/ci.sh [lint|test|all]
```

It works from any directory: it finds the repository root as the folder above `scripts/`
and runs from there.

**`lint`** runs ShellCheck (`shellcheck --severity=warning`) on every `*.sh` file under
`bin/` and `scripts/`, in every subfolder, one file at a time, so a bad file never hides
the next one. Files anywhere else (`test/`, `docs/`) are not linted.

```text
$ bash scripts/ci.sh lint
lint: ok (3 files)

$ bash scripts/ci.sh lint
bin/broken.sh:3:1: warning: Use 'cd ... || exit' in case cd fails. [SC2164]
lint: failed (1 of 4 files)
```

ShellCheck's own findings come first, then the last line is the summary; the exit status
is `0` when every file is clean and `1` otherwise.

**`test`** runs every `.bats` file under `test/` (subfolders included) with a JUnit report
for CI to keep: `bats --recursive --report-formatter junit --output reports test`, which
writes `reports/report.xml`. The `reports/` folder is emptied first, so an old report never
looks like a new one. The last line is `test: ok` (exit `0`) or `test: failed` (exit `1`);
no `.bats` file at all is a failure too: `test: no tests in test/`.

**`all`**, also what `ci.sh` does with no argument: `lint`, then `test`, stopping at the
first that fails (exit `1`, and the tests do not run when the lint failed). When both pass,
the last line is `ci: ok`.

Anything else (an unknown command, two arguments) prints `usage: ci.sh [lint|test|all]` on
stderr and exits `2`.
