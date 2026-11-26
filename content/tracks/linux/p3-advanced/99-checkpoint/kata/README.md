# Checkpoint 3: Go-Live Preflight

A new Debian 13 server is about to get its public IP. Write
`preflight.sh [-r ROOT] [-s SS_FILE] [-p PORTS]`, the check you run before that happens: it
reads the SSH and firewall configuration, the listening sockets and `/etc/fstab`, prints one
line per finding and exits like a monitoring probe.

```text
$ sudo bash preflight.sh
PASS ssh: PermitRootLogin no
FAIL ssh: PasswordAuthentication yes (want no)
PASS ssh: KbdInteractiveAuthentication no
PASS firewall: input policy drop
FAIL port: 5432 open on 0.0.0.0, [::]
WARN fstab: /data uses /dev/sdb1 (use UUID= or LABEL=)
WARN fstab: /data has no nofail
summary: 2 FAIL, 2 WARN
```

## Inputs

- `ROOT` is the system to check, `/` by default. The files are `ROOT/etc/ssh/sshd_config`,
  `ROOT/etc/nftables.conf` and `ROOT/etc/fstab` (the tests build a fake ROOT).
- `SS_FILE` holds the output of `ss -Htln`; without `-s`, run `ss -Htln` yourself.
- `PORTS` is a comma-separated list of the ports that may be public, `22,443` by default.

## SSH

Report `PermitRootLogin`, `PasswordAuthentication` and `KbdInteractiveAuthentication`, in
that order, as `PASS ssh: NAME no` or `FAIL ssh: NAME VALUE (want no)`. The value is the one
**sshd** would use, and sshd's rules are not the usual "last line wins":

- **For each keyword, the first value read is the one used.** Later lines with the same
  keyword are ignored.
- `Include PATH...` reads the matching files **at that point**, in lexical (`LC_ALL=C`)
  order, before the rest of the file. A path with glob characters may match many files or
  none. An absolute path is taken inside ROOT, and a relative one is relative to
  `ROOT/etc/ssh`. Included files can include others.
- Keywords ignore case, and a keyword and its value are separated by spaces or by `=`
  (`PasswordAuthentication=no`, `PasswordAuthentication = no`). Values are compared in
  lower case. Blank lines and lines whose first non-blank character is `#` are comments.
- A `Match` line starts conditional settings: from there on, nothing counts as global.
- A keyword that is never set has sshd's default: `prohibit-password` for
  `PermitRootLogin` and `yes` for the other two.

Debian's `sshd_config` starts with `Include /etc/ssh/sshd_config.d/*.conf`, so a drop-in
written by a cloud image wins over anything in the main file.

## Firewall

In `nftables.conf`, look at the **base chains on the input hook** (a chain whose body has
`hook input`). The `policy` statement can be on the same line as `type ... hook input` or on
its own line, and chains can be written on one line. Text after `#` is a comment.

- `PASS firewall: input policy drop` when an input chain has `policy drop`.
- `FAIL firewall: input policy is accept (want drop)` when there are input chains, but none
  drops (a chain without a `policy` accepts).
- `FAIL firewall: no input chain (want policy drop)` when there is none, or no file.

A `policy drop` on the `forward` or `output` chain does not protect the server.

## Ports

Each line of `ss -Htln` is `State Recv-Q Send-Q Local:Port Peer:Port`. Skip a header line
(first field `State`). The local address can be `0.0.0.0`, `*`, `10.0.0.5`, `[::]`,
`[::1]`, or have an interface after `%`, as in `127.0.0.53%lo:53`.

- A socket is local when its address is in `127.0.0.0/8` or is `::1`. Every other socket is
  **public**.
- Each public port not in `PORTS` gets `FAIL port: PORT open on ADDRESSES`. The addresses
  are written as in `ss`, without the `%interface`, in the order they first appear, each
  listed once and joined with `, `. The lines are sorted by port number.
- With no such port, print `PASS port: only allowed ports are public`.

## fstab

Skip comments, blank lines and entries of type `swap`. For the other entries, in file order:

- A device named by the kernel (`/dev/sd*`, `/dev/vd*`, `/dev/xvd*`, `/dev/hd*`,
  `/dev/nvme*`) gets `WARN fstab: MOUNT uses DEVICE (use UUID= or LABEL=)`: those names can
  change between boots. `/dev/mapper/...` (LVM) names are stable.
- A block device (`UUID=`, `LABEL=`, `PARTUUID=`, `PARTLABEL=` or a `/dev/` path) mounted
  anywhere other than `/`, `/boot`, `/boot/efi`, `/usr` or `/var` is a data disk. If its
  options do not include `nofail`, print `WARN fstab: MOUNT has no nofail`: without it, a
  missing disk stops the boot in emergency mode.
- With no warning, print `PASS fstab: stable names and nofail on data disks`.

## Summary and exit status

The last line is `summary: F FAIL, W WARN`. Exit `2` if there is any FAIL, `1` if there are
only warnings, and `0` otherwise.

An unknown option or any operand prints `usage: preflight.sh [-r ROOT] [-s SS_FILE] [-p PORTS]`
on stderr and exits `3`. A `PORTS` that is not a list of numbers prints
`preflight.sh: invalid port list: PORTS`, and an unreadable `sshd_config`, `fstab` or
`SS_FILE` prints `preflight.sh: cannot read PATH`. Both go to stderr and exit `3`.

There are 14 tests: 1 in `tests/visible.bats` and 13 hidden ones, plus ShellCheck.
