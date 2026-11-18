`accounts.sh [--root DIR] audit` looks for the accounts and homes an attacker would love.
It only reads: it takes no lock and changes nothing.

```text
$ bash accounts.sh --root image audit
backup: system account can log in
toor: uid 0
alice: .ssh/authorized_keys is writable by group or others (0664)
carol: home /home/carol is readable by others (0755)
```

Users come in passwd order, and the checks of one user in the order below. Each finding is
`NAME: MESSAGE`. With no finding, print `audit: no findings` and exit `0`; otherwise exit `1`.

## Every account

| Problem | Message |
| --- | --- |
| uid 0 and not `root`: a second root | `uid 0` |
| empty password field in shadow | `empty password` |
| a shell that is not in `/etc/shells` (comments aside), unless it is `/usr/sbin/nologin`, `/sbin/nologin`, `/bin/false` or `/usr/bin/false` | `shell SHELL is not in /etc/shells` |
| a system account (uid 1 to 999) with a login shell (one in `/etc/shells`) and a password field that does not start with `!` or `*`: a service account someone can log in as | `system account can log in` |

## Human users (uid 1000 to 59999)

Their home is the passwd field, under `DIR`. Modes are printed with four octal digits, as
`stat -c %a` gives them padded with zeros (`0755`, `2770`).

| Problem | Message |
| --- | --- |
| the home does not exist (no other check for that user) | `home HOME is missing` |
| the home is writable by group or others | `home HOME is writable by group or others (MODE)` |
| otherwise, others have any permission on it | `home HOME is readable by others (MODE)` |
| `HOME/.ssh` exists with any group or other bit (sshd refuses keys then) | `.ssh has mode MODE, expected 0700` |
| `HOME/.ssh/authorized_keys` is writable by group or others: anyone in the group could add a key | `.ssh/authorized_keys is writable by group or others (MODE)` |
| each regular file under the home that others may write, by path relative to the home, sorted | `world-writable file PATH` |

A real audit also checks who owns each home and key file. Here every file belongs to you, the
user running the tests, so owners are left out.

Tools: `stat -c %a`, bash arithmetic on octal numbers (`$(( 8#$mode & 8#022 ))`),
`printf '%04o'`, `find -perm -o+w`.
