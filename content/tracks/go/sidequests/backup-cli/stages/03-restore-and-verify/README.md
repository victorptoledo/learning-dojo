A backup you never restored is a hope, not a backup.

- `backup verify <src> <dst>` compares the two trees by **content** and prints one line per
  difference, in path order: `missing <path>` (in `<src>`, not in `<dst>`), `differs <path>`
  (different bytes) and `extra <path>` (only in `<dst>`). Exit 1 when there is any; otherwise
  it prints `ok: 3 files match` and exits with 0.
- `backup restore <dst> <target>` copies the backup into `<target>` with the same bytes,
  permissions and modification times, then prints `restored 3 files`. `<target>` may be
  missing or an empty folder; anything else is exit 1 with `error: target is not empty`, and
  nothing is written.

After `run`, `restore` and `verify`, the restored tree must `verify` against the original.
