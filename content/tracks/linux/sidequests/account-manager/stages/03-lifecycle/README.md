Accounts are locked when someone leaves on holiday and deleted when they leave the company.
And two admins (or two scripts) must never edit the files at the same time.

## The lock file

Every command that writes (`add`, `lock`, `unlock`, `del`) first takes `DIR/etc/.pwd.lock`,
as `vipw` and `useradd` do:

- create it **atomically**, failing if it exists (`set -C` makes `>` refuse to overwrite),
  and write your pid (`$$`) into it; remove it when the script exits, however it exits;
- if it exists and the pid in it is alive (`kill -0`), wait: try again every 0.1 s, for up to
  `ACCOUNTS_LOCK_WAIT` seconds (default `5`, decimals allowed), then give up with
  `accounts.sh: account files are locked by pid PID` (exit `1`), changing nothing;
- if the pid in it is gone, the lock is **stale**: print
  `accounts.sh: removing stale lock of pid PID` on stderr, remove it and carry on.

Checking "is there a lock?" and then creating it is two steps, and another process can slip
in between: let the creation itself be the check. With the lock, eight `add` started at the
same moment get eight different uids.

## Locking and unlocking

- `lock NAME` puts a `!` in front of the password field of `NAME`'s shadow line (the hash is
  kept, so `unlock` brings the same password back) and prints `locked NAME`. Already locked:
  print `NAME is already locked`, change nothing, exit `0`.
- `unlock NAME` removes that `!` and prints `unlocked NAME`. Not locked: `NAME is not locked`,
  exit `0`. If removing it would leave an empty field (a user made by `add` has only `!`),
  refuse: `accounts.sh: unlocking NAME would leave an empty password`, exit `1`; an empty
  password lets anyone in.

## Deleting

`del [-r] NAME` removes `NAME`'s passwd and shadow lines and `NAME` from every group's member
list, and prints `deleted NAME`. Its private group (named `NAME`, with the user's gid) goes
too, unless another user still has that gid as primary group. With `-r`, the home goes too,
but only a home under `/home/` (as given in passwd): for any other home print
`accounts.sh: not removing HOME: not under /home` on stderr and still delete the user. The
files are written as in `add` (`FILE+`, same mode, `FILE-`, rename).

Refusals, exit `1`: `accounts.sh: no such user: NAME` (also for `lock` and `unlock`), and
`accounts.sh: NAME is a system account` for a uid below 1000 or from 60000 up (`root`,
`daemon`, `nobody`).
