# Files, Directories, Links and Inodes

## Creating, copying, moving, deleting

```bash
mkdir -p app/config/env     # -p creates the parents and does not fail if it exists
touch notes.txt             # creates an empty file, or updates its modification time
cp notes.txt backup.txt     # copy a file
cp -r app app-copy          # copy a directory recursively (-a also keeps modes and times)
mv backup.txt old/          # move into a directory
mv draft.txt final.txt      # rename (same filesystem: instant, the data does not move)
rm final.txt                # delete: there is no recycle bin
rm -r app-copy              # delete a directory and everything inside
rmdir empty-dir             # delete only an empty directory
```

> [!WARNING]
> `rm` does not ask and does not keep a copy. Before `rm -r` with a glob or a variable,
> run `ls` with the same arguments. `rm -rf "$dir/"` with an empty `$dir` becomes `rm -rf /`:
> quote it and check it (bash's `${dir:?}` stops the script when it is empty).

Add `-i` to `cp`, `mv` and `rm` to be asked before overwriting or deleting, and `-v` to see
each file.

## Reading files

| Command | Use |
| --- | --- |
| `cat file` | print the whole file (small files) |
| `less file` | page through it: space, b, `/search`, `G` end, `q` quit |
| `head -n 20 file` | the first 20 lines |
| `tail -n 50 file` | the last 50 lines |
| `tail -f /var/log/app.log` | keep printing new lines as they are written (Ctrl+C stops) |
| `wc -l file` | count lines (`-w` words, `-c` bytes) |
| `file blob` | guess the type from the content, not the name |

## Inodes: names are not files

A file's data and metadata (owner, mode, size, times, where the blocks are) live in an
**inode**, identified by a number. A **directory** is a list of `name -> inode number`
entries. So a name is just a pointer to an inode:

```text
$ ls -li
1314 -rw-r--r-- 2 ada ada 12 Nov 14 10:02 report.txt
1314 -rw-r--r-- 2 ada ada 12 Nov 14 10:02 report-link.txt
```

The first column is the inode number, and the `2` after the mode is the **link count**: how
many names point to this inode. The data is freed only when the count reaches 0 **and** no
process still has the file open. That is why deleting a huge log that a program keeps
writing does not free the disk until the program closes it (Phase 4).

`stat file` shows all the inode fields.

## Hard links and symbolic links

```bash
ln report.txt report-hard.txt          # hard link: a second name for the same inode
ln -s /srv/app/releases/v42 current    # symbolic link: a small file holding a path
```

| | Hard link | Symbolic link |
| --- | --- | --- |
| Is | another name for the same inode | a file that contains a path |
| Target deleted | data still there through the other name | link is **dangling** (broken) |
| Directories | not allowed | allowed |
| Other filesystems | not allowed | allowed |
| `ls -l` shows | a normal file | `current -> /srv/app/releases/v42` |

Symbolic links are what you use day to day: switching releases, pointing `/etc/nginx/sites-enabled/app`
at `/etc/nginx/sites-available/app`, versions of a tool. `readlink -f current` resolves the
final path.

To replace a link **atomically**, create the new link under a temporary name and rename it
over the old one: `ln -s v43 current.tmp && mv -T current.tmp current`. A rename on the
same filesystem is atomic, so readers see the old target or the new one, never nothing.

## Finding files

`find` walks a tree and tests every entry:

```bash
find /var/log -name '*.log'                    # by name (quote the pattern!)
find . -type f -size +100M                     # regular files bigger than 100 MiB
find . -type d -name node_modules -prune       # directories, without descending into them
find /tmp -type f -mtime +7                    # modified more than 7 days ago
find . -type f -name '*.tmp' -delete           # delete what matches (test without -delete first)
find . -type f -name '*.sh' -exec chmod 755 {} +   # run a command on the matches
find . -type f -printf '%s %P\n'               # print size and path relative to the start
```

`-mtime +7` means "more than 7 whole days", `-7` less than 7. `-exec ... {} +` passes many
names to one command; `{} \;` runs the command once per file.

## Editing files: nano and vim

On a server you edit config files in the terminal.

- **nano** shows its shortcuts at the bottom: Ctrl+O saves, Ctrl+X quits, Ctrl+W searches.
- **vim** is everywhere. It has modes: you start in **normal** mode; `i` enters **insert**
  mode to type; **Esc** goes back. `:w` saves, `:q` quits, `:wq` both, `:q!` quits without
  saving, `/text` searches, `dd` deletes a line, `u` undoes.

Run `vimtutor` once: 30 minutes and vim stops being scary.

## Summary

- `mkdir -p`, `cp -r`/`cp -a`, `mv`, `rm -r`; there is no undo, so check before you delete.
- A name points to an inode; the inode holds metadata and data locations.
- Hard links are extra names for one inode; symbolic links store a path and may dangle.
- Switch a symbolic link atomically with a temporary link and `mv -T`.
- `find` searches by name, type, size and time, and can `-delete` or `-exec`.

## Further reading

- GNU Coreutils manual: <https://www.gnu.org/software/coreutils/manual/coreutils.html>
- `find(1)`: <https://man7.org/linux/man-pages/man1/find.1.html>
- `inode(7)`: <https://man7.org/linux/man-pages/man7/inode.7.html>
- *The Linux Command Line*, chapters 4 and 17: <https://linuxcommand.org/tlcl.php>
