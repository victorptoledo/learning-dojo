# Checkpoint 4: Final Exam — Incident Triage

The pager goes off: the API on `web1` is slow and some requests fail. Write
`triage.sh [-r ROOT]`, the script you run in the first minute. It reads the kernel's own
accounting, from `/proc`, `/sys/fs/cgroup`, the kernel log and `/boot`, and prints one line
per finding, so that you know where to look before you start guessing.

```text
$ sudo bash triage.sh
OK pressure cpu: some 3.20
CRIT pressure memory: some 41.50 full 18.20
OK pressure io: some 4.00 full 1.10
CRIT oom: 2 killed: java x2
WARN cgroup api.service: memory 1010 of 1024 MiB (98%)
CRIT cgroup api.service: oom kills 2
WARN fd 1234 java: deleted file still open: /var/log/app/app.log
WARN kernel: running 6.12.9-amd64, newest installed 6.12.12-amd64
OK reboot: not required
status: CRIT (3 crit, 3 warn)
```

`ROOT` is `/` by default; the tests build a fake one. Every path below is inside `ROOT`.
Each line is `LEVEL AREA: MESSAGE`, with `LEVEL` one of `OK`, `WARN` or `CRIT`, and the
areas come in the order of the sections below.

## 1. Pressure (`proc/pressure/cpu`, `memory`, `io`)

Pressure stall information (PSI) says which share of the last 10, 60 and 300 seconds tasks
spent **waiting** for a resource. Each file has two lines, and you use `avg10`:

```text
some avg10=41.50 avg60=30.12 avg300=12.00 total=987654321
full avg10=18.20 avg60=11.00 avg300=4.30 total=123456789
```

`some` means at least one task was waiting; `full` means all of them were, so nothing ran.

- `cpu`: `pressure cpu: some S`. `WARN` when `S` is at least `10`, `OK` otherwise. Ignore
  the `full` line of `cpu`: at the system level it carries no meaning.
- `memory` and `io`: `pressure NAME: some S full F`. `CRIT` when `F` is at least `10`, else
  `WARN` when `S` is at least `10`, else `OK`.
- Print the values as they appear in the file.
- If `proc/pressure` does not exist, print just `WARN pressure: not available (boot with psi=1)`.
  Debian builds PSI into the kernel but leaves it off unless the kernel command line turns
  it on.

## 2. OOM kills (`var/log/kern.log`)

Count the processes the OOM killer **killed**: the lines that contain
`Out of memory: Killed process PID (NAME)` (the whole system ran out) or
`Memory cgroup out of memory: Killed process PID (NAME)` (a cgroup hit its limit). The line
`NAME invoked oom-killer` names the process that asked for memory at the wrong moment, often
not the one that was killed, and `oom_reaper: reaped process ...` follows a kill already
counted. Neither one is a kill.

- `CRIT oom: N killed: NAME xC, NAME xC`: `N` is the total and each name has its count,
  sorted by count (highest first), then by name.
- `OK oom: no kills` when there are none, or when the log does not exist.

## 3. Service cgroups (`sys/fs/cgroup/system.slice/*.service`)

systemd gives each service its own cgroup. For each service directory, in name order, check
three things in this order, printing a line only for a problem:

- **Memory:** `memory.current` and `memory.max` hold bytes; `memory.max` is the word `max`
  when there is no limit. With a limit, `P = current * 100 / max` rounded down. From
  `P = 90` it is `WARN cgroup NAME: memory C of M MiB (P%)`, with MiB rounded down.
- **OOM kills inside the cgroup:** the `oom_kill` line of `memory.events`. Do not use the
  `oom` line (times the limit was hit) or `oom_group_kill`. More than `0` is
  `CRIT cgroup NAME: oom kills N`.
- **CPU throttling:** in `cpu.stat`, `nr_throttled` of `nr_periods` periods hit the CPU
  quota (`throttled_usec` is a time, not a count). When `nr_periods` is above `0` and
  `P = nr_throttled * 100 / nr_periods` (rounded down) is at least `25`:
  `WARN cgroup NAME: throttled in P% of periods`. Without a CPU quota there is no
  `nr_periods` line.

A missing file skips its check. With no problem in any service, print
`OK cgroups: no service near its limits`.

## 4. File descriptors (`proc/PID`)

For each process directory (all-digit names only), in **numeric** order:

- The open descriptors are the entries of `proc/PID/fd`. The soft limit is the first number
  on the `Max open files` line of `proc/PID/limits`. It can be `unlimited`, and then this
  check is skipped. At or above 80%:
  `WARN fd PID NAME: N of LIMIT open files (P%)`, with `NAME` from `proc/PID/comm` and `P`
  rounded down.
- Each entry of `fd` is a symbolic link. A target ending in ` (deleted)` is a file removed
  from the disk that the process still holds open, and its space comes back only when the
  process closes it. Print `WARN fd PID NAME: deleted file still open: PATH` (the path
  without the suffix) once per path, sorted by path. Paths may contain spaces.

Skip a process whose `fd` you cannot read (on a real system, other users' processes). With
no finding, print `OK fds: no process near its limit or holding deleted files`.

## 5. Kernel and reboot

- The running kernel is the first line of `proc/sys/kernel/osrelease`, and the installed
  ones are the files `boot/vmlinuz-VERSION`. Compare versions as versions: `6.12.12` is
  newer than `6.12.9`. When an installed version is newer than the running one, print
  `WARN kernel: running R, newest installed N`; otherwise `OK kernel: running R`. With no
  `osrelease`, print no kernel line.
- `run/reboot-required` exists when an upgrade (unattended or not) needs a reboot:
  `WARN reboot: required by P, Q` lists the packages of `run/reboot-required.pkgs`, each once
  and sorted (the file can repeat them), or `WARN reboot: required` without that file.
  Otherwise print `OK reboot: not required`.

## Status

The last line is `status: OK`, `status: WARN (W warn)` or `status: CRIT (C crit, W warn)`,
and the exit status is `0`, `1` or `2`. An unknown option or any operand prints
`usage: triage.sh [-r ROOT]` on stderr and exits `3`.

There are 13 tests: 1 in `tests/visible.bats` and 12 hidden ones, plus ShellCheck.
