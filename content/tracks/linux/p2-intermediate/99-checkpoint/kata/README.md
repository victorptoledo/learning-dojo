# Checkpoint 2: Backups with Rotation

Write `backup.sh [-k KEEP] [-n] SRC DEST`. It archives the directory `SRC` into `DEST` and
keeps only the newest `KEEP` archives of that directory.

```text
$ BACKUP_NOW=20261114-020000 bash backup.sh -k 2 /srv/app /backups
created /backups/app-20261114-020000.tar.gz
removed /backups/app-20261112-020000.tar.gz
```

## The archive

- Its name is `NAME-STAMP.tar.gz`, where `NAME` is the last part of `SRC` (`/srv/app/`
  gives `app`) and `STAMP` is the time as `YYYYmmdd-HHMMSS`. When the environment variable
  `BACKUP_NOW` is set, use it as the stamp (the tests do); otherwise use the current time.
- It is a gzip-compressed tar whose entries start with `NAME/` (archive with
  `tar -C <parent of SRC> NAME`).
- It appears **atomically**: write it to a temporary file inside `DEST` and `mv` it to its
  name. If anything fails, no temporary file is left behind.
- `DEST` is created if it does not exist.
- If an archive with that name already exists, print `backup.sh: already exists: PATH` on
  stderr and exit `1` without changing anything.
- On success print `created PATH`.

## Rotation

- The archives of this source are the files in `DEST` named exactly
  `NAME-YYYYmmdd-HHMMSS.tar.gz` (8 digits, a dash, 6 digits). Other files, and archives of
  other sources such as `app-v2-...`, are never touched.
- After creating the new archive, keep the newest `KEEP` of them (newest = latest stamp)
  and delete the others, printing `removed PATH` for each, **oldest first**.
- `-k KEEP` defaults to `5`. A value that is not a whole number of at least 1 prints
  `backup.sh: invalid keep: VALUE` on stderr and exits `2`.

## Dry run

With `-n` nothing is created, deleted or made: the script prints `would create PATH` and
`would remove PATH` instead, counting the archive it would create in the rotation.

## Paths and errors

`PATH` in the messages is `DEST/FILE` with `DEST` as given. An unknown option or not exactly
two operands prints `usage: backup.sh [-k KEEP] [-n] SRC DEST` on stderr and exits `2`. A
`SRC` that is not a directory prints `backup.sh: not a directory: SRC` and exits `1`.

There are 11 tests: 1 in `tests/visible.bats` and 10 hidden ones, plus ShellCheck.
