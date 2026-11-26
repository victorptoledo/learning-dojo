import pytest

from release import (
    CaseResult,
    Decision,
    Policy,
    Price,
    Rollout,
    Summary,
    Window,
    bucket,
    gate,
    kv_cache_gb,
    max_concurrency,
    percentile,
    self_host_usd_per_mtok,
    summarize,
    weights_gb,
)

# ---- 1. Serving ------------------------------------------------------------------------------


def test_weights_follow_precision():
    assert weights_gb(8, 16) == pytest.approx(16)
    assert weights_gb(70, 8) == pytest.approx(70)
    assert weights_gb(70, 4) == pytest.approx(35)


def test_kv_cache_counts_keys_and_values():
    # 8B-class model: 32 layers, 8 KV heads of 128 dimensions, 16-bit.
    assert kv_cache_gb(32, 8, 128, 1) == pytest.approx(131_072 / 1e9)
    assert kv_cache_gb(32, 8, 128, 8192) == pytest.approx(1.073741824)
    assert kv_cache_gb(32, 8, 128, 8192, bits=8) == pytest.approx(0.536870912)


def test_max_concurrency_on_one_gpu():
    assert max_concurrency(8, 16, 32, 8, 128, 8192, gpu_gb=24) == 5
    assert max_concurrency(8, 16, 32, 8, 128, 8192, gpu_gb=80) == 52
    assert max_concurrency(8, 16, 32, 8, 128, 8192, gpu_gb=80, reserve=0.0) == 59


def test_max_concurrency_when_the_weights_do_not_fit():
    assert max_concurrency(70, 16, 80, 8, 128, 8192, gpu_gb=80) == 0
    assert max_concurrency(70, 4, 80, 8, 128, 8192, gpu_gb=80) == 13


def test_self_hosting_cost_depends_on_utilisation():
    busy = self_host_usd_per_mtok(2.5, 1, 1000, 0.5)
    idle = self_host_usd_per_mtok(2.5, 1, 1000, 0.05)
    assert busy == pytest.approx(2.5 / 1.8)
    assert idle == pytest.approx(busy * 10)
    assert self_host_usd_per_mtok(2.5, 4, 1000, 0.5) == pytest.approx(busy * 4)


# ---- 2. Offline evaluation -------------------------------------------------------------------


def test_percentile_is_nearest_rank():
    assert percentile(list(range(1, 21)), 95) == 19
    assert percentile([30, 10, 20], 95) == 30
    assert percentile([4, 1, 3, 2], 50) == 2
    assert percentile([7], 95) == 7
    assert percentile([5, 1], 0) == 1
    assert percentile([], 95) == 0.0


def case(n, slice_="billing", passed=True, latency=800.0, tokens=(1000, 200), unsafe=False):
    return CaseResult(f"c{n}", slice_, passed, latency, tokens[0], tokens[1], unsafe)


def test_summarize_counts_slices_cost_and_safety():
    results = [
        case(1),
        case(2, passed=False, latency=1200),
        case(3, "refunds", tokens=(3000, 400)),
        case(4, "refunds", unsafe=True, latency=2000),
    ]
    summary = summarize(results, Price(3.0, 15.0))
    assert summary.pass_rate == pytest.approx(0.75)
    assert summary.slices == {"billing": (1, 2), "refunds": (2, 2)}
    assert summary.p95_latency_ms == 2000
    # mean cost per request: (6,000 + 6,000 + 15,000 + 6,000) / 4 / 1e6 USD
    assert summary.cost_per_1k == pytest.approx(33_000 / 4 / 1e6 * 1000)
    assert summary.unsafe == 1


def test_summarize_empty():
    assert summarize([], Price(1, 1)) == Summary(0.0, {}, 0.0, 0.0, 0)


def base(**overrides):
    values = {
        "pass_rate": 0.90,
        "slices": {"billing": (27, 30), "refunds": (18, 20), "tiny": (5, 5)},
        "p95_latency_ms": 2000.0,
        "cost_per_1k": 4.0,
        "unsafe": 0,
    }
    values.update(overrides)
    return Summary(**values)


def test_gate_passes_a_good_candidate_to_canary():
    assert gate(base(), base(pass_rate=0.92, cost_per_1k=2.0), "big-model-2026-10-01") == Decision("canary", ())


def test_gate_allows_the_limits_exactly():
    candidate = base(
        pass_rate=0.88,
        slices={"billing": (25, 29), "refunds": (17, 20), "tiny": (5, 5)},
        p95_latency_ms=3000.0,
        cost_per_1k=5.0,
    )
    assert gate(base(), candidate, "m-1") == Decision("canary", ())


def test_gate_blocks_unpinned_aliases():
    decision = gate(base(), base(), "big-model-latest")
    assert decision == Decision("block", ("model id is not pinned: big-model-latest",))


def test_gate_blocks_new_unsafe_answers():
    assert gate(base(unsafe=1), base(unsafe=3), "m-1").reasons == ("safety: 3 unsafe answers (baseline 1)",)
    assert gate(base(unsafe=2), base(unsafe=2), "m-1").verdict == "canary"


def test_gate_blocks_a_quality_drop():
    assert gate(base(), base(pass_rate=0.87), "m-1").reasons == ("quality: pass rate 0.90 -> 0.87",)


def test_gate_finds_a_slice_regression_hidden_by_the_average():
    candidate = base(pass_rate=0.91, slices={"billing": (22, 30), "refunds": (20, 20), "tiny": (5, 5)})
    assert gate(base(), candidate, "m-1").reasons == ("slice billing: 0.90 -> 0.73",)


def test_gate_ignores_small_and_missing_slices():
    candidate = base(slices={"billing": (27, 30), "refunds": (18, 20), "tiny": (0, 5), "new": (0, 40)})
    assert gate(base(), candidate, "m-1").verdict == "canary"
    smaller = base(slices={"billing": (10, 19), "refunds": (18, 20), "tiny": (5, 5)})
    assert gate(base(), smaller, "m-1").verdict == "canary"


def test_gate_checks_latency_and_cost():
    decision = gate(base(), base(p95_latency_ms=3200.4, cost_per_1k=5.01), "m-1")
    assert decision.reasons == (
        "latency: p95 3200 ms > 3000 ms",
        "cost: 4.00 -> 5.01 USD per 1k requests",
    )


def test_gate_uses_the_policy():
    strict = Policy(max_drop=0.0, max_slice_drop=0.0, min_slice_cases=1, max_p95_ms=1000, max_cost_increase=0.0)
    decision = gate(base(), base(pass_rate=0.89, cost_per_1k=4.1), "m-1", strict)
    assert decision.reasons == (
        "quality: pass rate 0.90 -> 0.89",
        "latency: p95 2000 ms > 1000 ms",
        "cost: 4.00 -> 4.10 USD per 1k requests",
    )


def test_gate_lists_every_reason_in_order():
    candidate = base(
        pass_rate=0.80,
        slices={"billing": (20, 30), "refunds": (10, 20), "tiny": (5, 5)},
        p95_latency_ms=4000.0,
        cost_per_1k=9.0,
        unsafe=1,
    )
    assert gate(base(), candidate, "x-latest") == Decision(
        "block",
        (
            "model id is not pinned: x-latest",
            "safety: 1 unsafe answers (baseline 0)",
            "quality: pass rate 0.90 -> 0.80",
            "slice billing: 0.90 -> 0.67",
            "slice refunds: 0.90 -> 0.50",
            "latency: p95 4000 ms > 3000 ms",
            "cost: 4.00 -> 9.00 USD per 1k requests",
        ),
    )


# ---- 3. Online rollout -----------------------------------------------------------------------


def test_bucket_is_stable_and_spread():
    assert [bucket(u) for u in ("u1", "alice", "user-0", "bob")] == [4042, 207, 567, 8650]
    assert all(0 <= bucket(f"user-{n}") < 10_000 for n in range(200))


def test_route_follows_the_stage():
    rollout = Rollout()
    assert rollout.percent == 1
    assert [rollout.route(u) for u in ("alice", "user-0", "bob")] == ["baseline"] * 3
    rollout.review(Window(1000), Window(100))
    assert rollout.percent == 5
    assert [rollout.route(u) for u in ("alice", "user-0", "bob")] == ["candidate", "baseline", "baseline"]
    rollout.review(Window(1000), Window(100))
    assert [rollout.route(u) for u in ("alice", "user-0", "bob")] == ["candidate", "candidate", "baseline"]


def test_stages_advance_then_finish():
    rollout = Rollout()
    healthy = (Window(1000, errors=10), Window(150, errors=1))
    assert [rollout.review(*healthy) for _ in range(5)] == ["advance", "advance", "advance", "done", "done"]
    assert rollout.state == "done"
    assert rollout.percent == 100
    assert rollout.route("bob") == "candidate"


def test_hold_until_enough_requests():
    rollout = Rollout(min_requests=200)
    assert rollout.review(Window(1000), Window(199, errors=50)) == "hold"
    assert rollout.percent == 1
    assert rollout.state == "running"


def test_rollback_on_more_errors():
    rollout = Rollout(stages=(5, 50, 100))
    assert rollout.review(Window(1000, errors=10), Window(100, errors=1)) == "advance"
    assert rollout.review(Window(1000, errors=10), Window(200, errors=5)) == "rollback"
    assert rollout.state == "rolled_back"
    assert rollout.percent == 0
    assert rollout.route("alice") == "baseline"
    assert rollout.review(Window(1000), Window(1000)) == "rolled_back"


def test_error_increase_at_the_limit_is_allowed():
    assert Rollout().review(Window(100, errors=0), Window(100, errors=1)) == "advance"
    assert Rollout().review(Window(0), Window(100, errors=1)) == "advance"
    assert Rollout().review(Window(0), Window(100, errors=2)) == "rollback"


def test_rollback_on_more_thumbs_down():
    assert Rollout().review(Window(1000, thumbs_down=30), Window(100, thumbs_down=5)) == "advance"
    assert Rollout().review(Window(1000, thumbs_down=30), Window(100, thumbs_down=6)) == "rollback"
    assert Rollout(max_thumbs_down_increase=0.1).review(Window(100), Window(100, thumbs_down=10)) == "advance"
