# Large Repositories: Shallow, Partial and Sparse, LFS and Maintenance

Git copies the whole history to every clone and checks out every file. That is wonderful for
a 50 MB project and painful for a monorepo with millions of files, years of history or big
binaries. Three things grow: **history** (commits and their trees), **blobs** (file contents,
especially binaries) and the **working tree** (the number of files). Each has its own fix.

## Less history: shallow clones

```bash
git clone --depth 1 https://github.com/acme/shop.git   # only the latest commit
git fetch --deepen 100                                  # 100 more commits of history
git fetch --unshallow                                   # all of it
```

Perfect for CI jobs that build one commit. The cost: `log`, `blame`, `merge-base` and
`bisect` only see the commits you have, and `git describe` may not find a tag (fetch with
`--tags` or more depth). GitHub Actions' `checkout` is shallow (depth 1) by default; set
`fetch-depth: 0` when a job needs history.

## Fewer blobs: partial clones

```bash
git clone --filter=blob:none https://github.com/acme/shop.git   # all commits and trees, blobs on demand
git clone --filter=tree:0 ...                                    # even trees on demand (for CI)
```

A **blobless clone** has the full history graph, so `log`, `merge-base` and branch work are
normal; file contents are downloaded the first time Git needs them (checkout, `diff`,
`blame`). It needs a server that supports it (GitHub, GitLab, Bitbucket do) and the network
for missing objects. It is the best default for big repositories on developer machines.

## Fewer files: sparse checkout

```bash
git clone --filter=blob:none --sparse https://github.com/acme/monorepo.git
cd monorepo
git sparse-checkout set services/payments libs/money   # cone mode: whole directories
git sparse-checkout add services/ledger
git sparse-checkout list
git sparse-checkout disable                             # everything again
```

Only those directories (plus the files at the root) are in the working tree, so `status`,
checkouts and the IDE get fast. Combined with a partial clone, you also download only the
blobs you need.

## Large binaries: Git LFS

Every version of a 200 MB video stays in every clone forever. **Git LFS** (Large File
Storage, an extension installed separately) stores large files on an LFS server and commits
only small **pointer files**:

```bash
git lfs install                    # once per machine
git lfs track "*.psd" "*.mp4"      # writes the patterns to .gitattributes
git add .gitattributes design/logo.psd
git commit -m "Track design files with LFS"
git lfs ls-files
```

```text
version https://git-lfs.github.com/spec/v1
oid sha256:4d7a214614ab2935c943f9e0ff69d22eadbb8f32b1258daaa5e2ca24d17e2393
size 209715200
```

LFS only helps for files tracked **before** they are committed. Big files already in history
stay there until you rewrite it (`git lfs migrate import`, or `git filter-repo`), which
changes every hash from that point on and needs the whole team to re-clone. Hosts also limit
file sizes (GitHub rejects files over 100 MB without LFS).

## Keeping Git fast

```bash
git maintenance start      # schedule background tasks: prefetch, commit-graph, loose objects, incremental repack
git gc                     # the classic one-off: pack loose objects, drop unreachable ones (after the reflog expires)
git count-objects -vH      # how big the repository is on disk
git config core.fsmonitor true        # a file-system watcher, so status does not scan every file
git config core.untrackedCache true
```

On disk, Git stores objects in **packfiles**, compressing similar objects as deltas of each
other; the **commit-graph** file speeds up history walks (`log --graph`, `merge-base`).
`scalar` (shipped with Git) clones and configures a large repository with all of these
settings in one command: `scalar clone URL`.

## Monorepo or polyrepo?

| | Monorepo | Many repositories |
| --- | --- | --- |
| Atomic changes across components | one commit, one review | coordinated releases |
| Shared tooling and versions | one place | duplicated, drifts |
| Repository size and speed | needs the techniques above | small and fast |
| Access control | coarse (CODEOWNERS helps) | per repository |
| CI | must build only what changed | simple per repository |

Neither is free: a monorepo needs partial clones, sparse checkout and a build system that
understands dependencies; many repositories need versioned packages and cross-repository
coordination.

## Summary

- Shallow clones cut history (good for CI); partial clones download blobs on demand (good
  for developers); sparse checkout limits the working tree.
- Git LFS replaces big binaries with pointer files, but only from the moment you track
  them; cleaning old history requires a coordinated rewrite.
- `git maintenance start`, the commit-graph, fsmonitor and `scalar` keep big repositories
  responsive.
- Monorepos trade simple atomic changes for repository-scale tooling.

## Further reading

- git-clone reference, --depth and --filter: <https://git-scm.com/docs/git-clone>
- Partial clone design notes: <https://git-scm.com/docs/partial-clone>
- git-sparse-checkout reference: <https://git-scm.com/docs/git-sparse-checkout>
- git-maintenance reference: <https://git-scm.com/docs/git-maintenance>
- Pro Git, "Packfiles": <https://git-scm.com/book/en/v2/Git-Internals-Packfiles>
- GitHub Docs, "About Git Large File Storage": <https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-git-large-file-storage>
- Atlassian, "How to handle big repositories with Git": <https://www.atlassian.com/git/tutorials/big-repositories>
- roadmap.sh topic "Git LFS": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/git-github/content/git-lfs@zdunaucVe8J1tKf_z-NQo.md>
