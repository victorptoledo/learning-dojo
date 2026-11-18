A rewrite you cannot take back is a bad tool. This stage gives `scrub.sh` a safety net and a
way to finish the job.

**Backup.** Right before `purge` or `redact` rewrite anything, every branch and tag is copied
under `refs/scrub-backup/`: `refs/heads/main` to `refs/scrub-backup/heads/main`,
`refs/tags/v1.0` to `refs/scrub-backup/tags/v1.0` (pointing at the same objects). A run that
refuses or has nothing to rewrite (`redacted 0 file versions`) makes no backup.

**One backup at a time.** While a backup exists, `purge` and `redact` refuse with
`scrub.sh: a backup exists: run undo or expire first` (exit `1`, nothing changed): a second
rewrite would bury the original history under the first one.

**Commit map.** Each rewrite writes `.git/scrub/commit-map` (inside the repository's Git
directory): one line `OLD NEW` per commit of the branches and tags it read, sorted by `OLD`.
A commit that kept its id maps to itself; a dropped commit maps to forty zeros. With it, a
team can translate the commit ids written in issues and pull requests.

**`scrub.sh undo REPO`** puts history back as it was before the rewrite: every branch and tag
of the backup points where it pointed, the backup refs and the commit map are removed, and
the index and working tree match `HEAD` again (purged files come back). Branches or tags
created after the rewrite are left alone. It prints `restored N refs`. Without a backup:
`scrub.sh: nothing to undo`, exit `1`. With uncommitted changes to tracked files it refuses
like a rewrite (`scrub.sh: uncommitted changes`).

**`scrub.sh expire REPO`** makes the rewrite final, on this clone: it deletes the backup
refs and the commit map, expires every reflog entry now and runs `git gc --prune=now`, so
the objects that only the old history used are gone from `.git`. It prints
`expired the backup and the reflogs` (and does the same with no backup). Afterwards
`git cat-file -e OLD_BLOB` fails for a purged file, and `git fsck` is still happy.

Why so many places? After a rewrite the old commits are still reachable from the backup
refs, from the reflog of each branch (`fast-import --force` and `reset --hard` both write
one) and from `ORIG_HEAD`. `gc` keeps everything reachable from any of them.
