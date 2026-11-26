# Checkpoint 4: Final Exam, the Model Migration

The support assistant runs on a hosted model that the provider retires next quarter. Two
candidates are on the table: a newer hosted model and an open-weight model you would serve
yourself. Write `release.py`, the tooling the team uses for every migration: size the self-hosted
option, decide from the offline evals whether a candidate may go to a canary, and run the canary
in stages with automatic rollback.

`CaseResult`, `Price`, `Summary`, `Policy`, `Decision` and `Window` are given in `release.py`.
Sizes are in GB of 10⁹ bytes.

## 1. Serving

- `weights_gb(params_billion, bits)`: the memory for the weights, `params × bits / 8`.
- `kv_cache_gb(layers, kv_heads, head_dim, tokens, bits=16)`: keys **and** values for every
  layer and KV head, `2 × layers × kv_heads × head_dim × tokens × bits / 8` bytes.
- `max_concurrency(params_billion, bits, layers, kv_heads, head_dim, context, gpu_gb, reserve=0.1)`:
  how many sequences of `context` tokens fit at once. Keep `reserve` of the GPU free (activations,
  the engine), take the weights at `bits`, and divide what is left by the KV cache of one full
  sequence, which stays **16-bit**. Round down; 0 when the weights do not fit.
- `self_host_usd_per_mtok(gpu_hourly_usd, gpus, tokens_per_second, utilisation)`: what a million
  tokens cost when the GPUs serve `tokens_per_second` while busy and are busy `utilisation` (0 to
  1) of the time. You pay for every hour.

## 2. Offline evaluation

- `percentile(values, p)`: nearest rank. Sort, take the value at rank `ceil(p / 100 × n)` (at
  least 1, counting from 1). An empty list gives `0.0`.
- `summarize(results, price) -> Summary`: the pass rate; `slices` as `{slice: (passed, total)}`;
  the p95 latency; `cost_per_1k`, the mean cost of one case in USD times 1,000; and the number of
  `unsafe` cases. No results: `Summary(0.0, {}, 0.0, 0.0, 0)`.
- `gate(baseline, candidate, model_id, policy=Policy()) -> Decision`: `"block"` with every reason
  that applies, in this order, or `"canary"` with no reasons. **A candidate is never promoted
  straight to everyone.**
  1. `"model id is not pinned: {model_id}"` when the id contains `latest`;
  2. `"safety: {n} unsafe answers (baseline {m})"` when the candidate has more unsafe answers;
  3. `"quality: pass rate {old:.2f} -> {new:.2f}"` when the pass rate drops by more than
     `max_drop`;
  4. `"slice {name}: {old:.2f} -> {new:.2f}"` for each slice in **both** summaries, by name, with
     at least `min_slice_cases` cases in each, whose pass rate drops by more than `max_slice_drop`;
  5. `"latency: p95 {p95:.0f} ms > {max:.0f} ms"` when the candidate's p95 is above `max_p95_ms`;
  6. `"cost: {old:.2f} -> {new:.2f} USD per 1k requests"` when the candidate costs more than
     `(1 + max_cost_increase)` times the baseline.

  A change exactly at a limit is allowed. Round differences to 6 decimals before comparing, so
  that `0.90 - 0.88` counts as `0.02`.

## 3. Online rollout

- `bucket(user_id) -> int`: the first 8 bytes of the SHA-256 of the UTF-8 id, as a big-endian
  integer, modulo 10,000. The same user always lands in the same bucket.
- `Rollout(stages=(1, 5, 25, 100), min_requests=100, max_error_increase=0.01,
  max_thumbs_down_increase=0.02)` starts at the first stage with `state == "running"`.
  - `percent`: the current stage's percentage; 0 after a rollback.
  - `route(user_id)`: `"candidate"` when `bucket(user_id) < percent × 100`, otherwise `"baseline"`.
  - `review(baseline, candidate) -> str`, with a `Window(requests, errors, thumbs_down)` of each
    arm since the last review:
    1. not running: return the state (`"done"` or `"rolled_back"`);
    2. fewer than `min_requests` candidate requests: `"hold"`;
    3. the candidate's error rate is more than `max_error_increase` above the baseline's, or its
       thumbs-down rate more than `max_thumbs_down_increase` above: the state becomes
       `"rolled_back"` and it returns `"rollback"` (rates of an arm with no requests are 0;
       round as in the gate);
    4. at the last stage: the state becomes `"done"` and it returns `"done"`;
    5. otherwise move to the next stage and return `"advance"`.

Run the visible tests with `uv run pytest`. There are 27 tests: 2 in `tests/test_visible.py` and
25 hidden ones, plus ruff on each module.
