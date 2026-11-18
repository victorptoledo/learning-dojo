Every command reads the image given by `--root DIR` (default `/`): `DIR/etc/passwd`,
`DIR/etc/group` and `DIR/etc/shadow`. If one of them cannot be read, print
`accounts.sh: cannot read FILE` on stderr and exit `2`. Empty lines are ignored everywhere.

## `accounts.sh [--root DIR] check`

Checks the three files and prints one line per problem, `FILE:LINE: MESSAGE`, where `FILE` is
`passwd`, `group` or `shadow`. Files come in that order, lines in file order, and the checks
of one line in the order of the table.

| File | Problem | Message |
| --- | --- | --- |
| passwd | not 7 fields (no other check for that line) | `expected 7 fields, got N` |
| passwd | name not matching `^[a-z_][a-z0-9_-]*$` or longer than 32 | `bad user name: NAME` |
| passwd | a name seen on an earlier line | `duplicate user: NAME` |
| passwd | a uid that is not a number | `bad uid: UID` |
| passwd | uid 0 for anyone but `root` | `uid 0 for NAME (only root may have uid 0)` |
| passwd | a uid of an earlier line | `uid UID already used by FIRST` |
| passwd | a gid that is not a number | `bad gid: GID` |
| passwd | a gid that no line of `group` has | `gid GID of NAME has no group` |
| group | not 4 fields (no other check) | `expected 4 fields, got N` |
| group | a name seen on an earlier line | `duplicate group: NAME` |
| group | a gid that is not a number | `bad gid: GID` |
| group | a gid of an earlier line | `gid GID already used by FIRST` |
| group | a member (comma-separated, 4th field) that is no user | `unknown member USER in GROUP` |
| shadow | not 9 fields (no other check) | `expected 9 fields, got N` |
| shadow | a name that is no user | `unknown user NAME` |
| shadow | an empty password field: anyone can log in | `empty password for NAME` |

After the lines of shadow, every user (in passwd order) that has no shadow line gets
`shadow: no entry for NAME`. With no problem at all, print `ok: U users, G groups` and exit
`0`; with problems, exit `1`. A user or group is counted once, even with a bad name.

## `accounts.sh [--root DIR] list`

The human users, those with a uid from 1000 to 59999, in passwd order:

```text
alice uid=1000 group=alice groups=sudo,users shell=/bin/bash
bob uid=1001 group=users groups=- shell=/bin/sh
```

`group` is the name of the primary group (the gid as a number if no group has it); `groups`
are the groups that list the user as a member, in group file order (`-` for none).

Any other command, or extra arguments, prints
`usage: accounts.sh [--root DIR] check|list|add|lock|unlock|del|audit [options] [NAME]`
on stderr and exits `2`. `awk -F:` with several files, `FNR` and `FILENAME` does most of the
work.
