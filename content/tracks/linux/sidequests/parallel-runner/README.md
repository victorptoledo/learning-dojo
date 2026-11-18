# Sidequest: Parallel job runner

A list of slow commands (resize these photos, compress these logs, call this API for each
customer) runs much faster a few at a time. You build `pjobs.sh`, a small cousin of
`xargs -P` and GNU `parallel`, in four stages:

1. run the commands of a job file, at most N at a time, starting a new one as soon as one ends;
2. keep each job's output and exit status, and kill a job that runs too long, together with
   every process it started;
3. stop cleanly on `SIGTERM`: no new jobs, the running ones stopped, no orphans left behind;
4. retry failed jobs with a growing delay, and resume a run without redoing what worked.

How it is checked:

- Your work is `pjobs.sh` at the top of the folder (bash, started as `bash pjobs.sh ...`).
  You may add more `.sh` files next to it; every script must pass ShellCheck.
- The tests write job files in a temporary directory, run your script (sometimes in the
  background, to send it a signal) and check what it prints, its exit status, the files it
  writes and which processes are still alive afterwards. The output formats in each stage's
  statement are the contract.
- Some tests measure time with generous margins (a job that should be killed after half a
  second must be gone within a few seconds). A run that hangs is stopped after 30 seconds.
- The tests run as an ordinary user in Debian 13, without network. A script started in the
  background by another script ignores `SIGINT`, so the tests stop it with `SIGTERM`.

Useful tools: `cmd &` and `$!`, `wait PID`, `kill -0`, `kill -- -PGID`, `setsid`, `trap`,
`$EPOCHREALTIME` and bash arrays. When you are done, `[A]` copies a review prompt: ask an AI
about your script.
