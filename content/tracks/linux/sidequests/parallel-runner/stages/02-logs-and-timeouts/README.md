Two options make the runner usable for real work.

## `-o DIR`: keep what each job did

`DIR` is created if needed (with its parents). For job `N`:

- `DIR/N.out` and `DIR/N.err` get its standard output and standard error;
- `DIR/N.cmd` gets its command line;
- when it ends, `DIR/N.status` gets one line: its exit status (`0`, `3`...) or `timeout`.

Without `-o`, the output is thrown away as before.

## `-t SECS`: kill a job that runs too long

`SECS` may have decimals (`-t 0.5`). A job still running `SECS` seconds after it started
gets `SIGTERM`; if it is still there `PJOBS_GRACE` seconds later (an environment variable,
default `2`, decimals allowed), it gets `SIGKILL`. It prints `timeout N (after SECSs): LINE`
(`SECS` as given), and it counts as a failure for the exit status.

```text
$ bash pjobs.sh -t 0.5 -o logs jobs.txt
ok 2: gzip -k logs/app-2.log
timeout 1 (after 0.5s): ./slow-upload.sh
2 jobs: 1 ok, 0 failed, 1 timed out
```

The summary gains `, T timed out` when at least one job timed out.

**Kill the whole job.** A job is often a shell that started other programs
(`curl ... | gzip > out`, or a script that runs `sleep 30 &`). Killing only the `bash -c`
leaves its children running. Start each job in a process group of its own (`setsid bash -c
LINE` makes the job's pid its group id) and signal the group: `kill -TERM -- -PID`. A job
that ignores `SIGTERM` must still be gone after the grace period.

A `SECS` that is not a positive number prints `pjobs.sh: bad timeout: SECS` and exits `2`.
