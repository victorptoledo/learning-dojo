"""Release tooling for a model migration: sizing, an offline gate and a staged canary."""

import hashlib
import math
from collections.abc import Sequence
from dataclasses import dataclass


@dataclass(frozen=True)
class CaseResult:
    case_id: str
    slice: str
    passed: bool
    latency_ms: float
    input_tokens: int
    output_tokens: int
    unsafe: bool = False


@dataclass(frozen=True)
class Price:
    input_per_mtok: float
    output_per_mtok: float


@dataclass(frozen=True)
class Summary:
    pass_rate: float
    slices: dict[str, tuple[int, int]]  # slice -> (passed, total)
    p95_latency_ms: float
    cost_per_1k: float  # USD per 1,000 requests
    unsafe: int


@dataclass(frozen=True)
class Policy:
    max_drop: float = 0.02
    max_slice_drop: float = 0.05
    min_slice_cases: int = 20
    max_p95_ms: float = 3000.0
    max_cost_increase: float = 0.25


@dataclass(frozen=True)
class Decision:
    verdict: str  # "canary" or "block"
    reasons: tuple[str, ...]


@dataclass(frozen=True)
class Window:
    requests: int
    errors: int = 0
    thumbs_down: int = 0


# ---- 1. Serving ------------------------------------------------------------------------------


def weights_gb(params_billion: float, bits: int) -> float:
    return params_billion * bits / 8


def kv_cache_gb(layers: int, kv_heads: int, head_dim: int, tokens: int, bits: int = 16) -> float:
    return 2 * layers * kv_heads * head_dim * tokens * bits / 8 / 1e9


def max_concurrency(
    params_billion: float,
    bits: int,
    layers: int,
    kv_heads: int,
    head_dim: int,
    context: int,
    gpu_gb: float,
    reserve: float = 0.1,
) -> int:
    free = gpu_gb * (1 - reserve) - weights_gb(params_billion, bits)
    if free <= 0:
        return 0
    return math.floor(free / kv_cache_gb(layers, kv_heads, head_dim, context))


def self_host_usd_per_mtok(gpu_hourly_usd: float, gpus: int, tokens_per_second: float, utilisation: float) -> float:
    tokens_per_hour = tokens_per_second * 3600 * utilisation
    return gpu_hourly_usd * gpus / tokens_per_hour * 1_000_000


# ---- 2. Offline evaluation -------------------------------------------------------------------


def percentile(values: Sequence[float], p: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    rank = max(1, math.ceil(p / 100 * len(ordered)))
    return ordered[rank - 1]


def summarize(results: Sequence[CaseResult], price: Price) -> Summary:
    if not results:
        return Summary(0.0, {}, 0.0, 0.0, 0)
    slices: dict[str, tuple[int, int]] = {}
    for result in results:
        passed, total = slices.get(result.slice, (0, 0))
        slices[result.slice] = (passed + result.passed, total + 1)
    cost = sum(r.input_tokens * price.input_per_mtok + r.output_tokens * price.output_per_mtok for r in results)
    return Summary(
        pass_rate=sum(r.passed for r in results) / len(results),
        slices=slices,
        p95_latency_ms=percentile([r.latency_ms for r in results], 95),
        cost_per_1k=cost / 1_000_000 / len(results) * 1000,
        unsafe=sum(r.unsafe for r in results),
    )


def gate(baseline: Summary, candidate: Summary, model_id: str, policy: Policy = Policy()) -> Decision:
    reasons: list[str] = []
    if "latest" in model_id:
        reasons.append(f"model id is not pinned: {model_id}")
    if candidate.unsafe > baseline.unsafe:
        reasons.append(f"safety: {candidate.unsafe} unsafe answers (baseline {baseline.unsafe})")
    if round(baseline.pass_rate - candidate.pass_rate, 6) > policy.max_drop:
        reasons.append(f"quality: pass rate {baseline.pass_rate:.2f} -> {candidate.pass_rate:.2f}")
    for name in sorted(baseline.slices.keys() & candidate.slices.keys()):
        old_passed, old_total = baseline.slices[name]
        new_passed, new_total = candidate.slices[name]
        if min(old_total, new_total) < policy.min_slice_cases:
            continue
        old_rate, new_rate = old_passed / old_total, new_passed / new_total
        if round(old_rate - new_rate, 6) > policy.max_slice_drop:
            reasons.append(f"slice {name}: {old_rate:.2f} -> {new_rate:.2f}")
    if candidate.p95_latency_ms > policy.max_p95_ms:
        reasons.append(f"latency: p95 {candidate.p95_latency_ms:.0f} ms > {policy.max_p95_ms:.0f} ms")
    if round(candidate.cost_per_1k - baseline.cost_per_1k * (1 + policy.max_cost_increase), 6) > 0:
        reasons.append(f"cost: {baseline.cost_per_1k:.2f} -> {candidate.cost_per_1k:.2f} USD per 1k requests")
    return Decision("block" if reasons else "canary", tuple(reasons))


# ---- 3. Online rollout -----------------------------------------------------------------------


def bucket(user_id: str) -> int:
    return int.from_bytes(hashlib.sha256(user_id.encode()).digest()[:8], "big") % 10_000


def _rate(count: int, requests: int) -> float:
    return count / requests if requests else 0.0


class Rollout:
    def __init__(
        self,
        stages: Sequence[float] = (1, 5, 25, 100),
        min_requests: int = 100,
        max_error_increase: float = 0.01,
        max_thumbs_down_increase: float = 0.02,
    ) -> None:
        self._stages = tuple(stages)
        self._stage = 0
        self._min_requests = min_requests
        self._max_error_increase = max_error_increase
        self._max_thumbs_down_increase = max_thumbs_down_increase
        self.state = "running"

    @property
    def percent(self) -> float:
        return 0.0 if self.state == "rolled_back" else self._stages[self._stage]

    def route(self, user_id: str) -> str:
        return "candidate" if bucket(user_id) < self.percent * 100 else "baseline"

    def review(self, baseline: Window, candidate: Window) -> str:
        if self.state != "running":
            return self.state
        if candidate.requests < self._min_requests:
            return "hold"
        errors = _rate(candidate.errors, candidate.requests) - _rate(baseline.errors, baseline.requests)
        thumbs = _rate(candidate.thumbs_down, candidate.requests) - _rate(baseline.thumbs_down, baseline.requests)
        if round(errors, 6) > self._max_error_increase or round(thumbs, 6) > self._max_thumbs_down_increase:
            self.state = "rolled_back"
            return "rollback"
        if self._stage == len(self._stages) - 1:
            self.state = "done"
            return "done"
        self._stage += 1
        return "advance"
