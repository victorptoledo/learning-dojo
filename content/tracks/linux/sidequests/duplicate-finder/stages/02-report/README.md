`dupes.sh report DIR` tells you how much space the duplicates waste, worst group first.

```text
$ bash dupes.sh report ~/Downloads
3 files of 1048576 bytes, 2097152 wasted:
iso/debian (1).iso
iso/debian.iso
old/debian.iso

2 files of 10 bytes, 10 wasted:
a.txt
b.txt

total: 2097162 bytes wasted in 2 groups
```

- A group is the same as in `scan`. Its header is `N files of SIZE bytes, W wasted:`, then
  its paths (sorted), then an empty line. Groups come by wasted space, largest first; equal
  ones by their first path. The last line is `total: W bytes wasted in G groups` (`1 group`
  when there is one). With no waste at all it is the only line: `total: 0 bytes wasted in 0 groups`.
- **Hard links are not copies.** Two paths with the same inode are one file stored once, so
  a group wastes its size once for every inode but one: three paths where two are hard links
  of each other waste one copy. A group whose paths are all the same inode wastes nothing and
  is not printed by `report` (`scan` still lists it: the content is the same).
- `--min-size N`, before `DIR`, makes `scan` and `report` ignore the files smaller than `N`
  bytes. `N` may end in `K` (×1024) or `M` (×1048576): `--min-size 4K`. A bad size prints
  `dupes.sh: bad size: N` on stderr and exits `2`; any other option is a usage error.

Tools: `find -printf '%s %i %P'`, `stat -c %i`, `sort -n -r`, and `awk` arrays to count the
distinct inodes of a group.
