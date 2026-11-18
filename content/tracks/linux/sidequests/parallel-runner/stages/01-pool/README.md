`pjobs.sh [-j N] JOBFILE` runs the jobs of `JOBFILE`, at most `N` at a time (default `2`).

```text
$ cat jobs.txt
# one command per line
gzip -k logs/app-1.log
gzip -k logs/app-2.log
grep -q ERROR logs/app-3.log
$ bash pjobs.sh -j 2 jobs.txt
ok 1: gzip -k logs/app-1.log
fail 3 (exit 1): grep -q ERROR logs/app-3.log
ok 2: gzip -k logs/app-2.log
3 jobs: 2 ok, 1 failed
```

- Every line is a job, run with `bash -c LINE`, except empty lines, lines of spaces and
  comments (`#`, maybe after spaces). Jobs are numbered from 1 in file order, counting only
  the jobs.
- At most `N` jobs run at once, and a new job starts **as soon as** a running one ends (not
  in batches of `N`). Jobs start in file order.
- Each job gets its number in the environment variable `PJOBS_JOB`. It reads its standard
  input from `/dev/null`; its output (stdout and stderr) is thrown away in this stage.
- When a job ends, print `ok N: LINE` (exit status 0) or `fail N (exit S): LINE`, so the
  lines come in the order the jobs end. Then print `J jobs: O ok, F failed` (`1 job` when
  there is one). The exit status is `0` when every job succeeded, `1` otherwise.

Errors: no `JOBFILE`, an unknown option or a missing option value prints
`usage: pjobs.sh [-j N] [-t SECS] [-r N] [-o DIR [--resume]] JOBFILE` on stderr and exits `2`
(the other options come in the next stages). An `N` that is not a positive integer prints
`pjobs.sh: bad job count: N`, and a job file that cannot be read prints
`pjobs.sh: cannot read JOBFILE`; both exit `2`.

A loop that starts jobs while fewer than `N` run, then looks for the ones that ended
(`kill -0 PID` fails once a job is gone; `wait PID` then gives its exit status), then sleeps
a moment, is enough. `wait -n` is another way.
