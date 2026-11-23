A canary nobody looks at is just a slower release. During every pause, after each `sleep
INTERVAL`, `run` now **measures** the canary: it runs `promq "QUERY"` with the spec's
`analysis.query`, exactly as written, and reads the number it prints (the canary's error
ratio).

| Answer | The check is | Printed (indented two spaces) |
| --- | --- | --- |
| a number `<= max` (`0.004`, `1e-4`) | good | `  check K/N: VALUE ok` |
| a number `> max` | failed | `  check K/N: VALUE failed` |
| `NaN`, nothing, anything that is not a number, or `promq` fails | inconclusive | `  check K/N: no data` |

`K/N` counts the checks of the current step. Checks are counted **in a row, over the whole
rollout** (a pause is often only two checks):

- a good check resets both counts;
- a failed check adds one failure and resets the inconclusive count;
- an inconclusive check adds one and leaves the failures as they are: not knowing is not
  good news.

When the failures reach `analysis.failure_limit`, or the inconclusive checks reach
`analysis.inconclusive_limit` (3 when the spec has none), the controller **rolls back** at
once, without waiting for the rest of the pause:

1. all the traffic back to stable: weight `0` for the canary, `100` for stable (the same
   patch as a step);
2. the canary Deployment scaled to `0` replicas;
3. it prints `rollout: SERVICE rolled back at step I: error ratio VALUE above MAX` (the
   failed check's value and the spec's `max`, as written), or `rollout: SERVICE rolled back
   at step I: no data from the analysis`, and exits **`3`**.

The last step has no pause, so it is never measured: reaching it is the promotion.

The tests script `promq`'s answers, call after call, and read the weights, the replicas,
how many times you slept and queried, and the exit status.
