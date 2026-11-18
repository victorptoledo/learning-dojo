# Sidequest: Duplicate finder

Years of downloads, photo imports and "copy of copy of report.pdf" fill a disk with files
that have exactly the same bytes. You build `dupes.sh`, a small cousin of `fdupes` and
`rdfind`, in three stages:

1. `scan`: find the groups of files with the same content, reading as little as possible;
2. `report`: how much space each group wastes, where two hard links to one inode are one
   file, not a copy;
3. `link`: replace every copy with a hard link to one of them, without ever losing data.

How it is checked:

- Your work is `dupes.sh` at the top of the folder (bash, started as `bash dupes.sh ...`).
  You may add more `.sh` files next to it; every script must pass ShellCheck.
- The tests build folders in a temporary directory, run your script and compare what it
  prints, its exit status and the files on disk (contents, inodes, permissions). The output
  formats in each stage's statement are the contract.
- Paths in the output are relative to the folder given, without `./`, and sorted in byte
  order (`LC_ALL=C sort`). File names may contain spaces or start with a dash; they never
  contain a newline.
- The tests run as an ordinary user in Debian 13, without network.

Useful tools: `find -type f -printf`, `stat -c`, `sha256sum`, `sort`, `uniq`, `awk`, `cmp`,
`ln` and `mv`. When you are done, `[A]` copies a review prompt: ask an AI about your script.
