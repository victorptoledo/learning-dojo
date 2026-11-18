# Sidequest: Folder mirror

Backing up a folder by copying everything again is slow, and copying over the old files in
place is risky: a crash in the middle leaves half-written files behind. Build `mirror`, a
command-line tool in the spirit of `rsync`, that makes a target folder an exact copy of a
source folder and only touches what changed.

```text
$ node bin.ts --dry-run --delete photos /mnt/backup/photos
copy 2026/beach.jpg
update albums.json
touch notes.txt
delete old/draft.jpg
copy 1, update 1, touch 1, delete 1, 2048371 bytes to transfer
$ node bin.ts --delete --exclude "*.tmp" photos /mnt/backup/photos
copied 2026/beach.jpg
updated albums.json
touched notes.txt
deleted old/draft.jpg
done: 4 ok, 0 failed
```

Everything lives in `mirror.ts`; `bin.ts` only connects `main` to the real process, so the
tests call `main(argv, io)` with a fake `Io` and look at the folders it leaves behind. The
contract tests build folders under the system's temporary directory, set modification times
with `utimes` and use `chmod` to make files unreadable (the check runs as a normal user, not
as root). The exported names and the output lines are fixed; everything behind them is yours.
Only Node.js itself: `node:fs`, `node:path`, `node:crypto`, `node:stream` and `node:util`.
`tests/` is yours too.
