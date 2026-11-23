A rollout takes minutes or hours. In that time the CI runner can die, someone can start a
second one, and someone will want to stop it. The controller now keeps **state** in the
folder `$ROLLOUT_STATE_DIR` (default `.rollout`, in the current directory), created when
needed, with three files per service:

| File | What |
| --- | --- |
| `SERVICE.state` | JSON: `{"status": ..., "step": I, "steps": N, "weight": W, "reason": ...}`, written under a temporary name in the same folder and renamed, never half-written |
| `SERVICE.lock` | held with `flock` by the run in progress; the kernel releases it when that process dies, however it dies |
| `SERVICE.abort` | created by `abort`, read by the run in progress |

`status` is `running`, `promoted`, `rolled-back` or `aborted`; `step` counts from 1 and is
the step entered last (or where it ended), `weight` its weight, `reason` why it rolled
back (empty otherwise).

**`run [--restart] FILE`**

- takes the lock without waiting (`flock -n`); when another run holds it, prints `rollout:
  SERVICE is already running` on stderr and exits **`4`**, touching nothing;
- removes an `abort` file left from before;
- with no state, starts at step 1. With a `running` state, the last run died mid-rollout:
  it prints `rollout: resuming SERVICE at step I/N` and **resumes at that step** (its weight
  again, its whole pause again), without redoing the steps before it. A finished rollout
  (`promoted`, `rolled-back`, `aborted`) is not run again: `rollout: SERVICE is already
  promoted; use run --restart to start again` (`rolled back`, `aborted`), exit `0`, no
  `kubectl` call. `--restart` starts at step 1 whatever the state;
- writes `running` and the step **before** moving the weights of each step, `promoted` at
  the end, and `rolled-back` with the reason when the analysis rolls back;
- after each `sleep` of a pause, **before** measuring: if `SERVICE.abort` exists, deletes
  it, rolls back as stage 3 does, saves `aborted`, prints `rollout: SERVICE aborted at
  step I` and exits `3`.

**`abort FILE`**

- a run holds the lock: create `SERVICE.abort`, print `rollout: abort requested for
  SERVICE`, exit `0` (the run stops within one interval);
- nobody holds it but the state is `running` (the run died): take the lock and roll back
  yourself, save `aborted`, print `rollout: SERVICE aborted at step I`, exit `0`;
- otherwise: `rollout: SERVICE is not running` on stderr, exit `1`.

**`status FILE`** prints one line and exits `0`:

```text
checkout: not started
checkout: running, step 2/3 (weight 50)
checkout: interrupted at step 2/3 (weight 50)      (running, but nobody holds the lock)
checkout: promoted
checkout: rolled back at step 2 (error ratio 0.04 above 0.01)
checkout: aborted at step 2
```

The tests start `run` in the background, hold it in a `sleep`, look at `status`, start a
second run, `abort` it, or kill it with `SIGKILL` (with every process of its group) and run
it again.
