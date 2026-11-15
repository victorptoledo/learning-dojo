# History Report: Reading a Repository from a Script

Turn a repository's history into a short report, the way release scripts and dashboards do.
Press **Enter** to create the kata folder and open it.

The lesson on staging and commits showed `git log` for humans. Scripts need output that does
not change with the user's configuration or Git's version: `rev-list`, `rev-parse` and
`log --format=...`, never the default `git log` layout.
