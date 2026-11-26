"""Release tooling for a model migration: sizing, an offline gate and a staged canary.

The data classes are given. Write the functions and the Rollout class.
"""

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
    raise NotImplementedError("todo: write weights_gb()")


def kv_cache_gb(layers: int, kv_heads: int, head_dim: int, tokens: int, bits: int = 16) -> float:
    raise NotImplementedError("todo: write kv_cache_gb()")


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
    raise NotImplementedError("todo: write max_concurrency()")


def self_host_usd_per_mtok(gpu_hourly_usd: float, gpus: int, tokens_per_second: float, utilisation: float) -> float:
    raise NotImplementedError("todo: write self_host_usd_per_mtok()")


# ---- 2. Offline evaluation -------------------------------------------------------------------


def percentile(values: Sequence[float], p: float) -> float:
    raise NotImplementedError("todo: write percentile()")


def summarize(results: Sequence[CaseResult], price: Price) -> Summary:
    raise NotImplementedError("todo: write summarize()")


def gate(baseline: Summary, candidate: Summary, model_id: str, policy: Policy = Policy()) -> Decision:
    raise NotImplementedError("todo: write gate()")


# ---- 3. Online rollout -----------------------------------------------------------------------


def bucket(user_id: str) -> int:
    raise NotImplementedError("todo: write bucket()")


class Rollout:
    def __init__(
        self,
        stages: Sequence[float] = (1, 5, 25, 100),
        min_requests: int = 100,
        max_error_increase: float = 0.01,
        max_thumbs_down_increase: float = 0.02,
    ) -> None:
        raise NotImplementedError("todo: set up the rollout")

    @property
    def percent(self) -> float:
        raise NotImplementedError("todo: write Rollout.percent")

    def route(self, user_id: str) -> str:
        raise NotImplementedError("todo: write Rollout.route()")

    def review(self, baseline: Window, candidate: Window) -> str:
        raise NotImplementedError("todo: write Rollout.review()")
