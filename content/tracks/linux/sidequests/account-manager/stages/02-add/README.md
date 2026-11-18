`accounts.sh [--root DIR] add [-c COMMENT] [-s SHELL] [-G GROUP,...] NAME` is your
`useradd -m`: it creates the user, a private group and a home.

```text
$ bash accounts.sh --root image add -c "Bob Builder" -G sudo bob
added bob (uid 1001, gid 1001)
```

## What it writes

- **uid:** the smallest number from 1000 up that no passwd line uses. **gid:** the same
  number if no group uses it, otherwise the smallest free gid from 1000 up.
- passwd gets `NAME:x:UID:GID:COMMENT:/home/NAME:SHELL` (default shell `/bin/bash`, empty
  comment by default); group gets `NAME:x:GID:` (the private group); shadow gets
  `NAME:!:DAYS:0:99999:7:::`, where `!` means "no password yet, cannot log in" and `DAYS` is
  today counted in days since 1970-01-01 (`$(( $(date +%s) / 86400 ))`).
- Every group of `-G` gets `NAME` at the end of its member list (`sudo:x:27:alice,bob`).
- The home `DIR/home/NAME` is a copy of `DIR/etc/skel`, dotfiles and subfolders included,
  each file keeping its mode; the home itself gets mode `0700`. Run as root, the real tool
  would also `chown -R UID:GID` it; as an ordinary user, skip that.

## Writing system files safely

These files are read all the time by every login. For each file you change:

1. write the new content to `FILE+` in the same folder;
2. give it the mode of `FILE` (`shadow` must stay `0640`, not become world-readable);
3. keep the old version as `FILE-` (that is what `/etc/passwd-` and `/etc/shadow-` are);
4. `mv FILE+ FILE`: a rename is atomic, so a reader sees the old file or the new one, never
   half of one. No `FILE+` may be left behind.

## Refusals

Nothing at all changes when the command is refused. Usage errors exit `2`: a name not matching
`^[a-z_][a-z0-9_-]{0,31}$` (`accounts.sh: bad user name: NAME`), a comment with `:` or a new
line (`accounts.sh: bad comment: COMMENT`). The others exit `1`:

- `accounts.sh: user NAME already exists`, or `group NAME already exists`;
- `accounts.sh: SHELL is not in /etc/shells` (the lines of `DIR/etc/shells` that are not
  comments);
- `accounts.sh: no such group: GROUP` for a group of `-G`;
- `accounts.sh: /home/NAME already exists`.

Afterwards, `check` must still say `ok`.
