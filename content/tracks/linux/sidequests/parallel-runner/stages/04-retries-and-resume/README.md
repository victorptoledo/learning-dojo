Networks fail and machines reboot. The last two options make a long run survive both.

## `-r N`: retry a failed job

A job that fails (non-zero exit or timeout) is run again, up to `N` more times. Instead of
its `fail`/`timeout` line, a failed attempt that will be retried prints
`retry J (attempt A of M): LINE`, where `A` is the attempt about to come and `M` is `N + 1`.
Only the last attempt prints `ok`, `fail` or `timeout`, and only it counts in the summary.

```text
$ PJOBS_BACKOFF=1 bash pjobs.sh -r 2 jobs.txt
retry 1 (attempt 2 of 3): curl -fsS https://example.com/report
ok 2: gzip -k logs/app-2.log
ok 1: curl -fsS https://example.com/report
2 jobs: 2 ok, 0 failed
```

- **Backoff.** The next attempt starts no sooner than `PJOBS_BACKOFF` seconds (default `1`,
  decimals allowed) after the failure, and the delay doubles each time: 1, 2, 4...
  (exponential backoff, so a struggling server gets room to recover).
- A job waiting for its retry does **not** hold a slot: other jobs run meanwhile. When its
  delay is over it goes before the jobs that have not started yet.
- Each attempt gets `PJOBS_ATTEMPT` (1, 2, ...) in its environment, next to `PJOBS_JOB`.
  With `-o`, each attempt overwrites `N.out`, `N.err` and `N.status`.

## `--resume`: do not redo what worked

`--resume` needs `-o DIR` (without it, it is a usage error). Before running, every job whose
`DIR/N.status` is `0` **and** whose `DIR/N.cmd` is still its command line is skipped: it
prints `skip N: LINE` (before anything else runs) and counts as `S skipped` in the summary,
which gains `, S skipped`. Every other job runs, including those that were `stopped` or
`timeout`, and the ones whose line changed in the job file.

```text
$ bash pjobs.sh -o logs --resume jobs.txt
skip 1: gzip -k logs/app-1.log
ok 2: gzip -k logs/app-2.log
2 jobs: 1 ok, 0 failed, 1 skipped
```

An `N` for `-r` that is not a whole number prints `pjobs.sh: bad retry count: N` and exits `2`.
