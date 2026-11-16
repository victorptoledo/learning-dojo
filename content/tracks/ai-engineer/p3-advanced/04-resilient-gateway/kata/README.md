# Resilient Gateway

Write `gateway.py`. Given (do not edit): `llm.py` (model types and `ScriptedModel`), `clock.py`
(`Clock`, a protocol with `now()` and `sleep(seconds)`, and `FakeClock`, whose `sleep` only moves
its time forward and records the delay), and, in `gateway.py`, the API errors, `Price`, `Route`,
`GatewayResult` and `GatewayError`.

The API errors: `RateLimitError(message, retry_after=None)`, `OverloadedError`, `ServerError` and
`APITimeoutError` are `retryable`; `BadRequestError` is not. All derive from `ModelAPIError`.

## Helpers

- `backoff_delay(attempt, base=0.5, cap=8.0, rng=random.random)`: full jitter,
  `rng() * min(cap, base * 2 ** attempt)`, for attempts numbered from 0.
- `estimate_tokens(request)`: the characters of the system prompt and of every message's text
  (string contents, and the `text` of `TextBlock`s and the `content` of `ToolResult`s) divided by 4
  (integer division), plus `request.max_tokens`.
- `cache_key(request)`: a stable hex SHA-256 of the request: equal requests give equal keys, any
  change (system, messages, tools, max_tokens, model) gives a different key. Hint:
  `json.dumps(dataclasses.asdict(request), sort_keys=True)`.
- `cost(usage, price)`: `input_tokens * price.input_per_million / 1e6 + output_tokens *
  price.output_per_million / 1e6`.

## `TokenBucket(capacity, refill_per_second, clock)`

Starts full. Tokens refill continuously at `refill_per_second`, never above `capacity`.

- `available()`: the tokens now (after refilling).
- `try_take(n)`: takes `n` tokens and returns `True` if there are enough, else `False` (takes none).
- `take(n)`: if there are not enough tokens, `clock.sleep` exactly as long as the refill needs,
  then takes them. `n` above the capacity raises `ValueError`.

## `Gateway(primary, fallback=None, clock=..., rng=random.random, max_retries=3, bucket=None)`

`complete(request) -> GatewayResult(response, route, attempts, cost, cached)`:

1. If an earlier call with the same `cache_key` succeeded with stop reason `end_turn`, return its
   response at once: `route` as before, `attempts=0`, `cost=0.0`, `cached=True`.
2. Try the primary route, then the fallback (if any). On each route make up to `max_retries + 1`
   calls. Before every call, if there is a bucket, `take(estimate_tokens(request))`.
3. A retryable error: if this was not the route's last attempt, sleep
   `backoff_delay(attempt)` (with the gateway's `rng`), or the error's `retry_after` if that is
   longer, then try again. After the last attempt, move on to the fallback without sleeping.
4. A `BadRequestError` is raised as it is, at once: retrying or falling back cannot fix it.
5. Success: return `GatewayResult(response, route.name, attempts, cost, cached=False)`, where
   `attempts` counts every call made for this request (on all routes) and `cost` uses that route's
   price. Add the cost to `gateway.spent[route.name]` (a dict, starting empty). Cache the response
   only when its stop reason is `end_turn`.
6. Every route failed: raise `GatewayError(message, attempts)` chained (`from`) to the last error.

Run the visible tests with `uv run pytest`. There are 21 tests: 2 in `tests/test_visible.py`
and 19 hidden ones, plus ruff on each module.
