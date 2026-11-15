# The Shell, the Filesystem Hierarchy and Getting Help

The shell is a program that reads a line, splits it into words, **expands** them (variables,
globs, `~`), finds the command and runs it. Knowing those steps explains most surprises you
will meet in bash.

## Paths and moving around

Linux has **one tree**, starting at `/`. There are no drive letters: other disks are
**mounted** somewhere inside the tree (Phase 3).

| Command | Does |
| --- | --- |
| `pwd` | print the working directory |
| `cd /var/log` | go to an **absolute** path (starts with `/`) |
| `cd log` or `cd ../lib` | go to a **relative** path, from where you are |
| `cd` or `cd ~` | go home (`/home/<you>`, or `/root` for root) |
| `cd -` | go back to the previous directory |
| `ls -lah` | list everything, long format, human sizes |

`.` is the current directory and `..` its parent. Names are **case-sensitive**: `Notes.txt`
and `notes.txt` are two files. Files whose names start with `.` are hidden from `ls` without
`-a`.

Press **Tab** to complete names and **Tab Tab** to see the choices; press **Ctrl+R** to
search your history, **Ctrl+C** to cancel the current command.

## Globs: the shell expands, not the command

```bash
ls *.log          # every name ending in .log in this directory
ls app-?.conf     # ? matches exactly one character
ls report-[0-9]*  # [..] matches one character from the set
echo ~/notes      # ~ becomes /home/you/notes
```

The shell replaces the pattern with the matching names **before** the command runs, so `ls`
never sees the `*`. If nothing matches, bash passes the pattern unchanged. Quote a pattern
to stop the expansion: `find . -name '*.log'` gives the `*` to `find`.

## Where things live: the Filesystem Hierarchy Standard

| Path | Holds |
| --- | --- |
| `/etc` | system-wide **configuration**, as text files (`/etc/ssh/sshd_config`, `/etc/hosts`) |
| `/var` | data that changes: **logs** in `/var/log`, caches, mail, databases in `/var/lib` |
| `/usr` | installed programs and libraries: `/usr/bin`, `/usr/lib`, `/usr/share/doc` |
| `/usr/local` | software you installed yourself, outside the package manager |
| `/opt` | self-contained third-party applications |
| `/home` | users' home directories; `/root` is root's |
| `/tmp` | temporary files, often cleared at boot |
| `/proc`, `/sys` | **virtual** files the kernel generates: processes, settings, devices |
| `/dev` | device files: disks (`/dev/sda`), `/dev/null`, `/dev/random` |
| `/boot` | the kernel and boot loader files |

On Debian 13, `/bin`, `/sbin` and `/lib` are links into `/usr` (the "merged /usr").

## How the shell finds a command

When you type `ls`, bash checks, in order: **aliases**, **functions**, **builtins** (`cd`,
`echo`, `type`), and finally searches the directories of `PATH`, left to right.

```text
$ echo "$PATH"
/usr/local/bin:/usr/bin:/bin
$ type cd ls ll
cd is a shell builtin
ls is /usr/bin/ls
ll is aliased to `ls -l'
$ command -v python3
/usr/bin/python3
```

`cd` must be a builtin: a separate program could only change its **own** directory. To run
a program that is not on `PATH`, give its path: `./deploy.sh`.

## Getting help

```bash
man ls              # the manual page: q quits, /word searches, n goes to the next match
man 5 crontab       # section 5: the crontab FILE format, not the command
ls --help           # short summary from most GNU tools
help cd             # builtins are documented by bash itself
apropos partition   # search manual page descriptions (same as man -k)
```

Manual sections: **1** user commands, **5** file formats, **7** overviews (`man 7 signal`),
**8** administration commands. When a page mentions `crontab(5)`, the number is the
section.

## Summary

- One tree from `/`; absolute paths start with `/`, relative ones from the working directory.
- The shell expands globs and `~` before running the command; quote to prevent it.
- Configuration in `/etc`, changing data and logs in `/var`, programs in `/usr`.
- Lookup order: alias, function, builtin, then `PATH`; `type` tells you which one wins.
- `man`, `--help`, `help` and `apropos` answer most questions offline.

## Further reading

- Filesystem Hierarchy Standard 3.0: <https://refspecs.linuxfoundation.org/FHS_3.0/fhs/index.html>
- GNU Bash manual, "Shell Expansions": <https://www.gnu.org/software/bash/manual/bash.html>
- *The Linux Command Line*, part 1: <https://linuxcommand.org/tlcl.php>
- Linux man pages online: <https://man7.org/linux/man-pages/index.html>
