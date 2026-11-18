# Sidequest: Offline account manager

`useradd`, `usermod`, `passwd -l` and `userdel` edit three plain text files that every login
reads: `/etc/passwd`, `/etc/group` and `/etc/shadow`. You build `accounts.sh`, a small
version of those tools that works on a **system image**: a folder with an `etc/` and a
`home/`, like a container's root filesystem or a disk mounted for repair. In four stages:

1. check the three files and list the human users;
2. add a user with its private group and a home from `/etc/skel`, writing the files the way
   the real tools do (temporary file, same mode, backup, atomic rename);
3. take a lock file so two admins never edit at once, lock and unlock passwords, and delete
   users;
4. audit the image: second roots, empty passwords, service accounts that can log in, open
   homes and `.ssh` folders.

How it is checked:

- Your work is `accounts.sh` at the top of the folder (bash, started as
  `bash accounts.sh --root DIR COMMAND ...`). You may add more `.sh` files next to it; every
  script must pass ShellCheck.
- The tests build a small Debian 13 image in a temporary directory (root, a few system
  accounts, `nobody` and `alice`), run your script on it and compare what it prints, its exit
  status and the files (contents, modes, backups). The formats in each stage's statement are
  the contract.
- The tests run as an ordinary user without network, so the script never needs root: the image
  is yours. Never run your script without `--root` on a real machine.

The fields, from `man 5 passwd`, `man 5 group` and `man 5 shadow`:

```text
passwd  name:x:uid:gid:comment:home:shell
group   name:x:gid:member1,member2
shadow  name:password:lastchange:min:max:warn:inactive:expire:reserved
```

When you are done, `[A]` copies a review prompt: ask an AI about your script and its safety.
