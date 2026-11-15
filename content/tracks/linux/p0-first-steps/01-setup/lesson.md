# Linux, Distributions and Your First Shell

Almost every server you will deploy to runs Linux: cloud virtual machines, Kubernetes nodes,
and the base image of nearly every container. This track teaches Linux the way a backend or
DevOps engineer uses it: through the **shell**, on **Debian 13 "trixie"**, with scripts you
can test.

## Kernel, userland, distribution

**Linux** is strictly the **kernel**: the program that owns the hardware, schedules
processes, manages memory and files, and enforces permissions. Programs talk to it through
**system calls** (`open`, `read`, `fork`, `execve`, ...).

Everything you type commands into is **userland**: the shell (`bash`), the GNU core utilities
(`ls`, `cp`, `sort`), the init system (`systemd`), libraries such as `glibc`.

A **distribution** packages a kernel with a userland and adds:

| A distribution decides | Debian 13 | Fedora / RHEL | Alpine |
| --- | --- | --- | --- |
| Package format and tool | `.deb`, `apt` | `.rpm`, `dnf` | `.apk`, `apk` |
| Init system | systemd | systemd | OpenRC |
| C library | glibc | glibc | musl |
| Release policy | stable, about every 2 years | Fedora 6 months / RHEL 10 years | every 6 months |

Ubuntu is built from Debian, so what you learn here applies to it almost unchanged. When
Fedora or RHEL differ in a way that matters, the lessons say so.

> [!NOTE]
> Linux is not Unix, but it follows the same design and the **POSIX** standard, so a
> portable shell script or C program works on Linux, macOS and the BSDs.

## Why servers run Linux

- **No licence cost** per machine, which matters at thousands of machines.
- **Everything is a text file or a command**, so everything can be automated and versioned.
- **Containers are a Linux kernel feature** (namespaces and cgroups, Phase 4).
- The cloud providers, Kubernetes and most databases are built and tested on it first.

## Getting a Debian shell on Windows

You need a real Linux shell to practise. Two good ways:

**WSL 2** (Windows Subsystem for Linux) runs a real Linux kernel in a lightweight VM:

```powershell
wsl --install -d Debian      # once, then reboot if asked
wsl -d Debian                # opens a bash shell
```

**Docker** gives you a throwaway Debian 13 in seconds, and it is what the dojo uses to check
your katas:

```powershell
docker run -it --rm debian:trixie bash
```

`-it` attaches your terminal, `--rm` deletes the container when you leave. Anything you
install there disappears with it, which is perfect for experiments.

Use WSL for daily practice (your files stay, editors integrate with it) and Docker when you
want a clean machine or to reproduce exactly what a server or the dojo runs.

## The prompt and your first commands

```text
dojo@debian:~$ whoami
dojo
dojo@debian:~$ pwd
/home/dojo
dojo@debian:~$ uname -sr
Linux 6.12.48+deb13-amd64
dojo@debian:~$ cat /etc/os-release
PRETTY_NAME="Debian GNU/Linux 13 (trixie)"
...
```

The prompt shows **user@host:directory**. `~` is your home directory, and `$` means a normal
user (`#` means root). A command is a program name followed by **arguments**, separated by
spaces. Options usually start with `-` (short, combinable: `ls -la`) or `--` (long:
`ls --all`).

## Packages with apt

Software comes from the distribution's **repositories**, signed and tested together:

```bash
sudo apt update               # refresh the package lists
apt search ripgrep            # find a package
sudo apt install ripgrep      # install it and its dependencies
apt show ripgrep              # version, size, description
dpkg -L ripgrep               # which files it installed
sudo apt upgrade              # upgrade everything installed
sudo apt remove ripgrep       # remove (keep config); purge removes config too
```

`apt` resolves dependencies; `dpkg` is the lower-level tool that installs one `.deb` file.
Inside a Docker container you are root, so there is no `sudo`.

## How the dojo checks bash katas

Linux katas are **bash scripts**. When you save a `.sh` file in the kata folder, the dojo
runs it inside the `dojo-linux` container (Debian 13, bash 5.2, no network) and:

1. checks that every script parses (`bash -n`), the shell's "does not compile";
2. runs the tests with **bats** (Bash Automated Testing System), which calls your script
   with arguments and compares its output and exit status;
3. runs **ShellCheck**, a linter that finds the classic bash bugs. Each script is one more
   test that must pass.

The starter scripts print `todo: ...` and exit with an error until you write them.

> [!WARNING]
> Bash reads Windows line ends (`\r\n`) as part of each command and fails with errors such as
> `$'\r': command not found`. The dojo strips them before the check, but real servers will
> not. Save `.sh` files with **LF** line ends (VS Code: the `CRLF`/`LF` button in the status
> bar; the dojo's workspace already sets LF for shell scripts).

## Summary

- Linux is the kernel; a distribution adds packages, an init system and a release policy.
- Debian 13 uses `apt`, systemd and glibc; Ubuntu inherits almost all of it.
- Get a shell with `wsl -d Debian` or `docker run -it --rm debian:trixie bash`.
- `apt update` then `apt install`; `dpkg -L` lists a package's files.
- Katas are bash scripts checked by `bash -n`, bats and ShellCheck, with LF line ends.

## Further reading

- *The Linux Command Line* by William Shotts, free online: <https://linuxcommand.org/tlcl.php>
- The Debian Administrator's Handbook: <https://debian-handbook.info/browse/stable/>
- Debian 13 "trixie" release information: <https://www.debian.org/releases/trixie/>
- Debian Reference, chapter on packages: <https://www.debian.org/doc/manuals/debian-reference/>
