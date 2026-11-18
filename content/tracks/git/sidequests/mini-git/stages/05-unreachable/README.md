An amended commit, a deleted branch, a dropped stash, a `git add` you never committed: their
objects stay in `.git` until `gc` prunes them, and until then they can be recovered. The last
command finds them, the way `git fsck --unreachable` does.

**`minigit.sh unreachable REPO`** prints one line `unreachable TYPE ID` per object of the
repository that nothing reaches, sorted by `ID`:

```text
$ bash minigit.sh unreachable ~/src/shop
unreachable blob 2f1e0c...
unreachable commit 9a3b77...
unreachable tree c0de11...
```

**Every object.** Objects live in two places, and you must read both yourself:

- **loose**, one file each: `.git/objects/2f/1e0c...` is the object `2f1e0c...`;
- **packed**: each `.git/objects/pack/pack-*.pack` has an index `pack-*.idx` listing the
  ids it holds; `git show-index < pack-*.idx` prints them (`OFFSET ID (CRC)`).

`git cat-file --batch-check` gives the types of many objects in one process. Using
`--batch-all-objects` to skip this part is not allowed (the tests check).

**What reaches an object.** Start from these roots:

- every ref (`git for-each-ref`: branches, tags, remotes, `refs/stash`...);
- `HEAD`, even when detached;
- every reflog entry, old and new id: the files under `.git/logs/`, one line per move,
  `OLD NEW Name <email> TIME ZONE<TAB>message` (forty zeros mean "none");
- the index (`git ls-files --stage`): files staged but not committed are not lost.

From a commit you reach its tree and its parents; from a tree, its entries (but not the
commit of a submodule, mode `160000`: it lives in another repository); from an annotated
tag, the object it points at. Everything not reached that way is unreachable, objects
reached only from other unreachable objects included.

Plumbing allowed: `cat-file`, `rev-parse`, `for-each-ref`, `show-index` and `ls-files`. The
tests compare your output with `git fsck --unreachable` (which also ignores `ORIG_HEAD`). A
repository with nothing unreachable prints nothing. A wrong number of arguments or a `REPO`
that is not a Git repository is the usage error (`2`).
