import pytest

from clock import FakeClock
from gateway import (
    BadRequestError,
    Gateway,
    GatewayError,
    OverloadedError,
    Price,
    RateLimitError,
    Route,
    ServerError,
    TokenBucket,
    backoff_delay,
    cache_key,
    cost,
    estimate_tokens,
)
from llm import MAX_TOKENS, Message, Request, Response, ScriptedModel, TextBlock, ToolCall, ToolResult, ToolSpec, Usage

BIG = Price(3.0, 15.0)
SMALL = Price(0.5, 2.5)


def request(text="What is RAG?", **kwargs):
    return Request((Message("user", text),), **kwargs)


def ok(text="ok", usage=Usage(1_000, 200)):
    return Response((TextBlock(text),), usage=usage)


def gateway(primary_script, fallback_script=None, **kwargs):
    clock = kwargs.pop("clock", FakeClock())
    primary = ScriptedModel(primary_script)
    fallback = ScriptedModel(fallback_script) if fallback_script is not None else None
    gw = Gateway(
        Route("big", primary, BIG),
        Route("small", fallback, SMALL) if fallback is not None else None,
        clock=clock,
        rng=lambda: 1.0,
        **kwargs,
    )
    return gw, primary, fallback, clock


# ---- helpers -----------------------------------------------------------------------------------


def test_backoff_caps_and_jitters():
    assert backoff_delay(10, rng=lambda: 1.0) == 8.0
    assert backoff_delay(3, rng=lambda: 0.25) == 1.0
    assert backoff_delay(2, base=1.0, cap=3.0, rng=lambda: 0.5) == 1.5
    assert backoff_delay(0, rng=lambda: 0.0) == 0.0


def test_estimate_tokens():
    req = Request(
        (
            Message("user", "a" * 40),
            Message("assistant", (TextBlock("b" * 20), ToolCall("c1", "t", {"x": 1}))),
            Message("user", (ToolResult("c1", "c" * 20),)),
        ),
        system="s" * 20,
        max_tokens=100,
    )
    assert estimate_tokens(req) == (40 + 20 + 20 + 20) // 4 + 100


def test_cache_key_is_stable_and_sensitive():
    assert cache_key(request()) == cache_key(request())
    assert len(cache_key(request())) == 64
    others = [
        request("What is RAG??"),
        request(system="Be brief."),
        request(max_tokens=10),
        request(model="other"),
        request(tools=(ToolSpec("t", "d", {"type": "object"}),)),
    ]
    keys = {cache_key(r) for r in others}
    assert len(keys) == len(others)
    assert cache_key(request()) not in keys


def test_cost():
    assert cost(Usage(1_000_000, 0), BIG) == pytest.approx(3.0)
    assert cost(Usage(2_000, 400), BIG) == pytest.approx(0.006 + 0.006)


# ---- token bucket -----------------------------------------------------------------------------------


def test_bucket_refills_over_time_up_to_capacity():
    clock = FakeClock()
    bucket = TokenBucket(100, 10, clock)
    assert bucket.try_take(80) is True
    assert bucket.available() == pytest.approx(20)
    assert bucket.try_take(30) is False
    assert bucket.available() == pytest.approx(20)
    clock.advance(2)
    assert bucket.available() == pytest.approx(40)
    clock.advance(60)
    assert bucket.available() == pytest.approx(100)


def test_bucket_take_sleeps_exactly_as_needed():
    clock = FakeClock()
    bucket = TokenBucket(100, 10, clock)
    bucket.take(100)
    assert clock.sleeps == []
    bucket.take(30)
    assert clock.sleeps == [pytest.approx(3.0)]
    assert bucket.available() == pytest.approx(0)


def test_bucket_rejects_more_than_capacity():
    with pytest.raises(ValueError):
        TokenBucket(100, 10, FakeClock()).take(101)


# ---- gateway ---------------------------------------------------------------------------------------


def test_success_on_the_first_try():
    gw, primary, _, clock = gateway([ok()])
    result = gw.complete(request())
    assert result.response.text == "ok"
    assert (result.route, result.attempts, result.cached) == ("big", 1, False)
    assert result.cost == pytest.approx(cost(Usage(1_000, 200), BIG))
    assert gw.spent == {"big": pytest.approx(result.cost)}
    assert clock.sleeps == []


def test_retries_transient_errors_with_backoff():
    gw, primary, _, clock = gateway([OverloadedError(), ServerError(), ok()])
    result = gw.complete(request())
    assert result.attempts == 3
    assert clock.sleeps == [0.5, 1.0]
    assert len(primary.requests) == 3


def test_retry_after_wins_when_longer():
    gw, _, _, clock = gateway([RateLimitError(retry_after=12), RateLimitError(retry_after=0.1), ok()])
    gw.complete(request())
    assert clock.sleeps == [12, 1.0]


def test_uses_the_gateway_random_function():
    clock = FakeClock()
    model = ScriptedModel([ServerError(), ok()])
    gw = Gateway(Route("big", model, BIG), clock=clock, rng=lambda: 0.5)
    gw.complete(request())
    assert clock.sleeps == [0.25]


def test_bad_requests_are_not_retried_or_sent_to_the_fallback():
    gw, primary, fallback, _ = gateway([BadRequestError("prompt is too long")], [ok()])
    with pytest.raises(BadRequestError):
        gw.complete(request())
    assert len(primary.requests) == 1
    assert fallback.requests == []


def test_falls_back_after_the_last_retry_without_sleeping():
    gw, primary, fallback, clock = gateway([ServerError()] * 3, [ok("from small", Usage(500, 100))], max_retries=2)
    result = gw.complete(request())
    assert result.route == "small"
    assert result.response.text == "from small"
    assert result.attempts == 4
    assert clock.sleeps == [0.5, 1.0]
    assert result.cost == pytest.approx(cost(Usage(500, 100), SMALL))
    assert gw.spent == {"small": pytest.approx(result.cost)}


def test_all_routes_fail():
    gw, _, _, _ = gateway([ServerError("down")] * 2, [OverloadedError("busy")] * 2, max_retries=1)
    with pytest.raises(GatewayError) as caught:
        gw.complete(request())
    assert caught.value.attempts == 4
    assert isinstance(caught.value.__cause__, OverloadedError)


def test_no_fallback_configured():
    gw, primary, _, _ = gateway([ServerError()] * 4)
    with pytest.raises(GatewayError) as caught:
        gw.complete(request())
    assert caught.value.attempts == 4
    assert len(primary.requests) == 4


def test_caches_complete_answers():
    gw, primary, _, _ = gateway([ok("first"), ok("second")])
    first = gw.complete(request())
    again = gw.complete(request())
    assert again.response == first.response
    assert (again.cached, again.attempts, again.cost, again.route) == (True, 0, 0.0, "big")
    assert len(primary.requests) == 1
    other = gw.complete(request("Something else"))
    assert other.response.text == "second"
    assert gw.spent["big"] == pytest.approx(2 * first.cost)


def test_does_not_cache_truncated_answers():
    cut = Response((TextBlock("half"),), MAX_TOKENS, Usage(10, 10))
    gw, primary, _, _ = gateway([cut, ok("full")])
    assert gw.complete(request()).response.text == "half"
    assert gw.complete(request()).response.text == "full"
    assert len(primary.requests) == 2


def test_cached_fallback_answer_keeps_its_route():
    gw, _, _, _ = gateway([ServerError()], [ok("small answer")], max_retries=0)
    gw.complete(request())
    assert gw.complete(request()).route == "small"


def test_bucket_paces_the_calls():
    clock = FakeClock()
    bucket = TokenBucket(capacity=1_000, refill_per_second=100, clock=clock)
    gw, primary, _, _ = gateway([ok("a"), ok("b")], clock=clock, bucket=bucket)
    first = request("x" * 400, max_tokens=500)
    second = request("y" * 400, max_tokens=500)
    gw.complete(first)
    gw.complete(second)
    assert estimate_tokens(first) == 600
    assert clock.sleeps == [pytest.approx(2.0)]
