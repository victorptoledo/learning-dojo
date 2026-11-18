Someone presses Ctrl-C, or `systemctl stop` sends `SIGTERM`. Killing the runner alone is the
worst answer: its jobs live on as orphans, half done, and nobody waits for them.

On `SIGTERM` or `SIGINT`, `pjobs.sh`:

1. starts no new job;
2. sends `SIGTERM` to the process group of every running job, and `SIGKILL` to the groups
   still alive `PJOBS_GRACE` seconds later (as for a timeout);
3. waits for them, printing `stopped N: LINE` for each one as it ends (with `-o`, its
   `N.status` is `stopped`);
4. prints `interrupted: S stopped, W not started` and exits `143` after `SIGTERM` or `130`
   after `SIGINT` (128 plus the signal number, as a shell reports a job killed by it).

```text
$ bash pjobs.sh -j 2 jobs.txt
ok 1: ./resize.sh photo-001.jpg
^C
stopped 2: ./resize.sh photo-002.jpg
stopped 3: ./resize.sh photo-003.jpg
interrupted: 2 stopped, 7 not started
```

After it exits, no process of any job may be left. It must react within a fraction of a
second, even while every slot is busy with a job that would run for minutes.

Bash runs a `trap` only between commands, so a script that sleeps for a long time, or that
`wait`s for one job only, reacts late; short sleeps in the main loop (or `sleep N & wait $!`)
keep it responsive.
