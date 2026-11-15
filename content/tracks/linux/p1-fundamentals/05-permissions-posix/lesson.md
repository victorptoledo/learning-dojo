# POSIX Permissions, sudo and User Management

Every file has an **owner** (a user), a **group**, and a **mode**: what the owner, the group
members and everyone else may do with it. The kernel checks the mode on every `open`,
`exec` and directory operation.

## Users and groups

```text
$ id
uid=1000(ada) gid=1000(ada) groups=1000(ada),27(sudo),1001(devs)
$ grep ada /etc/passwd
ada:x:1000:1000:Ada Lovelace:/home/ada:/bin/bash
```

`/etc/passwd` lists accounts (`name:x:uid:gid:comment:home:shell`); the password hashes are
in `/etc/shadow`, readable only by root. `/etc/group` lists groups and their members. User
**root** has uid 0 and bypasses permission checks. System services run as their own users
(`www-data`, `postgres`) so that a hole in one cannot touch the others' files.

```bash
sudo useradd --create-home --shell /bin/bash bruno   # create a user (adduser is the friendly Debian wrapper)
sudo passwd bruno                                    # set the password
sudo groupadd devs                                   # create a group
sudo usermod -aG devs bruno                          # ADD to a group (-a: without -a the other groups are removed)
sudo userdel -r bruno                                # delete the user and the home directory
```

New group membership applies to **new logins**: log out and in again (or `newgrp devs`).

## Reading a mode

```text
$ ls -l
-rwxr-x--- 1 ada devs 4096 Nov 14 10:00 deploy.sh
drwxrwsr-x 2 ada devs 4096 Nov 14 10:00 shared
```

The first character is the type (`-` file, `d` directory, `l` symbolic link), then three
triplets: **owner** `rwx`, **group** `r-x`, **others** `---`.

| | On a file | On a directory |
| --- | --- | --- |
| `r` | read the content | **list** the names inside |
| `w` | change the content | **create, delete, rename** entries inside |
| `x` | run it as a program | **enter** it and reach the entries by name |

So deleting a file needs `w` on the **directory**, not on the file. And a directory with
`r` but no `x` lets you see names but not open anything.

## chmod: symbolic and octal

Each triplet is a number: `r`=4, `w`=2, `x`=1, added up.

| Octal | Symbolic | Typical use |
| --- | --- | --- |
| `644` | `rw-r--r--` | normal files |
| `600` | `rw-------` | private files: SSH keys, secrets |
| `755` | `rwxr-xr-x` | programs and directories |
| `750` | `rwxr-x---` | readable by the group only |
| `700` | `rwx------` | private directories (`~/.ssh`) |

```bash
chmod 640 app.conf                # set exactly
chmod u+x deploy.sh               # add execute for the owner (u owner, g group, o others, a all)
chmod go-w notes.txt              # remove write for group and others
chmod -R u=rwX,go=rX site/        # recursive; X = execute only for directories (and files already executable)
chown www-data:www-data site/     # change owner and group (root only for the owner)
chgrp devs report.txt             # change the group (you must be a member)
stat -c '%a %U:%G %n' app.conf    # 640 ada:ada app.conf
```

## umask: the default mode

New files are created with mode `666` and directories with `777`, minus the **umask**. With
the usual umask `022`, files get `644` and directories `755`. With `027`, files get `640`
and others get nothing. `umask` prints it; set it in `~/.profile` or for a service in its
systemd unit (`UMask=0027`).

## The special bits

A fourth octal digit goes in front:

| Bit | Octal | On a file | On a directory |
| --- | --- | --- | --- |
| **setuid** | `4000` | runs with the **owner's** identity (`/usr/bin/passwd` runs as root) | (ignored) |
| **setgid** | `2000` | runs with the file's group | new entries **inherit the directory's group** |
| **sticky** | `1000` | (ignored) | only the owner of an entry may delete or rename it (`/tmp` is `1777`) |

`ls -l` shows them in the execute position: `rwsr-xr-x` (setuid), `rwxr-sr-x` (setgid),
`rwxrwxrwt` (sticky). A capital `S` or `T` means the bit is set but execute is not.

> [!WARNING]
> A setuid-root program with a bug gives root to whoever runs it. Audit them:
> `find / -xdev -type f -perm -4000`. Prefer **capabilities** for single privileges, such as
> letting a program bind port 80: `setcap cap_net_bind_service=+ep /usr/local/bin/app`.

## sudo: root, but only when needed

Do not log in as root. Users in the `sudo` group (Debian; `wheel` on Fedora/RHEL) run single
commands as root and every use is logged:

```bash
sudo apt update
sudo -u postgres psql          # run as another user
sudo -i                        # a root shell, when you really need one
```

Rules live in `/etc/sudoers` and `/etc/sudoers.d/*`. Always edit them with **`visudo`**,
which checks the syntax: a broken sudoers file can lock everyone out of root.

```text
# /etc/sudoers.d/deploy: the deploy user may restart the app, and nothing else
deploy ALL=(root) NOPASSWD: /usr/bin/systemctl restart app.service
```

When owner, group and others are not enough (two groups need different rights), **ACLs**
add entries per user or group: `setfacl -m g:auditors:r report.txt`, `getfacl report.txt`.

## Summary

- Owner, group, others, each with r, w, x; for directories x means enter and w means create
  or delete inside.
- Octal: r=4, w=2, x=1; `644` files, `755` programs and directories, `600` secrets.
- `chmod`, `chown`, `chgrp`, `stat`; `umask 022` gives `644` and `755` by default.
- setuid runs as the owner, setgid on a directory shares its group, sticky protects entries.
- `sudo` with rules edited by `visudo`; `usermod -aG` adds to a group.

## Further reading

- Arch Wiki, "File permissions and attributes": <https://wiki.archlinux.org/title/File_permissions_and_attributes>
- `chmod(1)`: <https://man7.org/linux/man-pages/man1/chmod.1.html> and `credentials(7)`: <https://man7.org/linux/man-pages/man7/credentials.7.html>
- `sudoers(5)` on Debian: <https://manpages.debian.org/trixie/sudo/sudoers.5.en.html>
- *The Linux Command Line*, chapter 9: <https://linuxcommand.org/tlcl.php>
