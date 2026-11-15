# Git's Object Model: Blobs, Trees, Commits and SHA Hashes

Most Git surprises disappear once you know what Git actually stores. It is not a list of
diffs: it is a small **key-value database of immutable objects**, plus a few **references**
that name some of them.

## Content addressing

Every object is stored under the **hash of its content** (SHA-1 by default, 40 hex
characters; SHA-256 repositories exist since Git 2.29). You can compute it yourself:

```bash
$ echo "hello" | git hash-object --stdin
ce013625030ba8dba906f756967f9e9ca394464a
$ echo "hello" | git hash-object -w --stdin   # -w also writes it to .git/objects
$ git cat-file -p ce0136                       # print an object by (a prefix of) its hash
hello
```

Three consequences matter every day:

- **Same content, same hash.** Ten files with identical content are stored once.
- **Objects never change.** Changing one byte gives a different hash, so a "changed" object
  is always a new object.
- **Integrity.** If a disk error flips a bit, the hash no longer matches and Git notices.

## The four object types

| Type | Stores | Points to |
| --- | --- | --- |
| **blob** | the bytes of one file; no name, no permissions | nothing |
| **tree** | one directory: entries of mode, type, hash and **name** | blobs and other trees |
| **commit** | a tree, zero or more parents, author, committer, dates, message | one tree, its parents |
| **tag** (annotated) | an object, a tag name, the tagger, a message, an optional signature | usually a commit |

```text
$ git cat-file -p HEAD
tree 9c4b1e0d2f...
parent 51ae2c0b7a...
author Ada Lovelace <ada@example.com> 1763112000 +0000
committer Ada Lovelace <ada@example.com> 1763112000 +0000

Add search to the catalogue

$ git cat-file -p 9c4b1e0d2f
100644 blob 8f94139338...    README.md
040000 tree 3b18e512db...    src
```

A commit is a **snapshot**: its tree describes every file of the project. Files that did
not change simply point to the same blobs as the previous commit, so snapshots are cheap.
Diffs are computed when you ask for them (`git diff`, `git show`). On disk Git later packs
objects and stores similar ones as deltas (`git gc`), but that is a storage detail: the
model stays "snapshots".

## The commit graph is a DAG

Each commit names its **parents**. A normal commit has one, the first commit has none and a
merge commit has two or more. Following parents gives a **directed acyclic graph** (DAG):
directed because the arrows point to the past, acyclic because a commit cannot be its own
ancestor (its hash would have to contain itself).

```text
A <- B <- C <- E      main
      \       /
       <- D <-        (E is a merge with parents C and D)
```

Because a commit's hash covers its parent's hash, **a commit pins its whole history**.
Change anything in `B` and `B`, `C`, `D` and `E` all get new hashes. That is why rewriting
pushed history is a big deal (Phase 2).

## References: names for commits

Hashes are hard to remember, so Git keeps **refs**: small files holding a hash.

| Ref | File | Moves when |
| --- | --- | --- |
| branch `main` | `.git/refs/heads/main` | you commit on it, merge into it, reset it |
| tag `v1.0.0` | `.git/refs/tags/v1.0.0` | never, by convention |
| remote-tracking `origin/main` | `.git/refs/remotes/origin/main` | you fetch |
| `HEAD` | `.git/HEAD` | you switch branches |

`HEAD` is usually a **symbolic ref**: it contains `ref: refs/heads/main`, meaning "the
current branch is main". When you commit, Git writes the new commit with the old tip as its
parent and moves `main`; `HEAD` follows because it points to `main`. If `HEAD` holds a hash
directly you are in **detached HEAD** state (Phase 1, branches lesson).

```bash
cat .git/HEAD                    # ref: refs/heads/main
git rev-parse main               # the hash main points to
git show-ref                     # every ref with its hash (packed ones too)
git log --oneline --graph --all  # draw the DAG
```

Creating a branch writes one small file, which is why branches in Git are nearly free.

## Plumbing and porcelain

Commands such as `hash-object`, `cat-file`, `rev-parse`, `ls-tree` and `update-ref` are
**plumbing**: stable, script-friendly building blocks. `add`, `commit`, `log` and `switch`
are **porcelain** for humans, and their output can change between versions. Scripts (like
your katas) should prefer plumbing or the porcelain options meant for scripts
(`--porcelain`, `--format`).

## Summary

- Git is a content-addressed object store: the key is the hash of the content.
- Blobs hold file bytes, trees hold names and structure, commits hold a tree, parents and
  metadata, annotated tags hold a name and message for an object.
- Commits form a DAG; a commit's hash covers its whole ancestry.
- Branches, tags and `HEAD` are references: names that point to commits.

## Further reading

- Pro Git, "Git Objects": <https://git-scm.com/book/en/v2/Git-Internals-Git-Objects>
- Pro Git, "Git References": <https://git-scm.com/book/en/v2/Git-Internals-Git-References>
- git-cat-file reference: <https://git-scm.com/docs/git-cat-file>
- Git's hash function transition (SHA-256): <https://git-scm.com/docs/hash-function-transition>
- roadmap.sh topic "HEAD": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/head@V8nLIvddyOKTzEjOJX5GW.md>
