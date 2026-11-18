`dupes.sh link [--dry-run] [--min-size N] DIR` reclaims the space: in each group, every
copy becomes a hard link to the group's **first path** (the keeper).

```text
$ bash dupes.sh link ~/Downloads
linked iso/debian.iso -> iso/debian (1).iso
skipped old/debian.iso: different permissions
linked b.txt -> a.txt
reclaimed: 1048586 bytes
```

- Groups in `scan` order, paths in order inside a group. A path that already has the
  keeper's inode is left alone and prints nothing.
- **Never lose data.** Hard links share one inode, so they share the permission bits and the
  owner too: a copy whose mode, owner or group differs from the keeper's is not linked and
  prints `skipped PATH: different permissions`. Right before linking, compare the two files
  byte by byte (`cmp`): a file that changed since it was hashed prints `skipped PATH: changed`.
- **Never a missing file.** Do not `rm` the copy and then `ln`: if the script dies between the
  two, the file is gone. Create the link under a temporary name in the copy's folder, then
  `mv` it over the copy (a rename is atomic). No temporary file may be left behind.
- A copy that cannot be replaced (for example, its folder is read-only) prints
  `dupes.sh: cannot link PATH` on stderr; the others are still linked, and the exit status
  is `1` at the end. Otherwise it is `0`.
- The last line is `reclaimed: B bytes`: the space `report` counted as wasted for the copies
  that were linked (copies that were hard links of each other count once).
- `--dry-run` changes nothing on disk and prints `would link PATH -> KEEPER` lines and
  `would reclaim: B bytes` instead. It is only valid for `link` (a usage error elsewhere).

Run `report` afterwards: the groups you linked are gone from it.
