import pytest

from resilience import CircuitBreaker, CircuitOpen, ResilienceError, RetryExhausted, retry, timer


class FakeClock:
    def __init__(self, now: float = 100.0) -> None:
        self.now = now

    def __call__(self) -> float:
        return self.now


def failing(error: BaseException, counter: list):
    def call():
        counter.append(1)
        raise error

    return call


def test_retry_gives_up_with_retry_exhausted_chained_to_the_last_error():
    calls = []
    wrapped = retry(times=4, sleep=lambda s: None)(failing(TimeoutError("slow"), calls))
    with pytest.raises(RetryExhausted) as caught:
        wrapped()
    assert len(calls) == 4
    assert caught.value.attempts == 4
    assert isinstance(caught.value.__cause__, TimeoutError)
    assert isinstance(caught.value, ResilienceError)


def test_retry_sleeps_with_exponential_backoff_between_attempts():
    pauses = []
    wrapped = retry(times=4, delay=0.5, backoff=3, sleep=pauses.append)(failing(OSError(), []))
    with pytest.raises(RetryExhausted):
        wrapped()
    assert pauses == [0.5, 1.5, 4.5]


def test_retry_does_not_retry_other_exceptions():
    calls = []
    wrapped = retry(times=5, on=ConnectionError, sleep=lambda s: None)(failing(KeyError("x"), calls))
    with pytest.raises(KeyError):
        wrapped()
    assert len(calls) == 1


def test_retry_accepts_a_tuple_of_exceptions():
    calls = []
    wrapped = retry(times=2, on=(ConnectionError, TimeoutError), sleep=lambda s: None)(failing(TimeoutError(), calls))
    with pytest.raises(RetryExhausted):
        wrapped()
    assert len(calls) == 2


def test_retry_passes_arguments_and_keeps_metadata():
    @retry(times=2, sleep=lambda s: None)
    def add(a, b=0):
        """Add two numbers."""
        return a + b

    assert add(2, b=3) == 5
    assert add.__name__ == "add"
    assert add.__doc__ == "Add two numbers."
    assert add.__wrapped__(1) == 1


def test_retry_rejects_bad_times_when_the_decorator_is_created():
    with pytest.raises(ValueError):
        retry(times=0)


def test_breaker_opens_after_threshold_consecutive_failures():
    clock = FakeClock()
    breaker = CircuitBreaker(threshold=2, reset_after=10, clock=clock)
    assert breaker.state == "closed"
    for _ in range(2):
        with pytest.raises(OSError):
            with breaker:
                raise OSError("down")
    assert breaker.state == "open"
    with pytest.raises(CircuitOpen):
        with breaker:
            pytest.fail("the block must not run while the circuit is open")


def test_breaker_success_resets_the_failure_count():
    breaker = CircuitBreaker(threshold=2, clock=FakeClock())
    with pytest.raises(OSError):
        with breaker:
            raise OSError()
    with breaker:
        pass
    with pytest.raises(OSError):
        with breaker:
            raise OSError()
    assert breaker.state == "closed"


def test_breaker_half_open_trial_closes_on_success():
    clock = FakeClock()
    breaker = CircuitBreaker(threshold=1, reset_after=10, clock=clock)
    with pytest.raises(OSError):
        with breaker:
            raise OSError()
    clock.now += 9.9
    assert breaker.state == "open"
    clock.now += 0.1
    assert breaker.state == "half-open"
    with breaker:
        pass
    assert breaker.state == "closed"


def test_breaker_half_open_trial_reopens_on_failure():
    clock = FakeClock()
    breaker = CircuitBreaker(threshold=3, reset_after=10, clock=clock)
    for _ in range(3):
        with pytest.raises(OSError):
            with breaker:
                raise OSError()
    clock.now += 10
    with pytest.raises(OSError):
        with breaker:
            raise OSError("still down")
    assert breaker.state == "open"
    clock.now += 5
    assert breaker.state == "open", "the timer restarts when the trial fails"


def test_breaker_does_not_suppress_exceptions():
    breaker = CircuitBreaker(threshold=5, clock=FakeClock())
    with pytest.raises(ZeroDivisionError):
        with breaker:
            1 / 0


def test_breaker_as_a_decorator():
    clock = FakeClock()
    breaker = CircuitBreaker(threshold=1, reset_after=10, clock=clock)
    calls = []

    @breaker
    def fetch(x):
        """Fetch x."""
        calls.append(x)
        if x < 0:
            raise ConnectionError()
        return x * 2

    assert fetch(2) == 4
    assert fetch.__name__ == "fetch"
    with pytest.raises(ConnectionError):
        fetch(-1)
    with pytest.raises(CircuitOpen):
        fetch(3)
    assert calls == [2, -1]


def test_breaker_rejects_bad_thresholds():
    with pytest.raises(ValueError):
        CircuitBreaker(threshold=0)


def test_timer_measures_the_block():
    clock = FakeClock(5.0)
    with timer(clock) as timing:
        assert timing.elapsed is None
        clock.now = 7.5
    assert timing.elapsed == 2.5


def test_timer_sets_elapsed_and_re_raises_when_the_block_fails():
    clock = FakeClock(1.0)
    with pytest.raises(RuntimeError):
        with timer(clock) as timing:
            clock.now = 4.0
            raise RuntimeError("boom")
    assert timing.elapsed == 3.0
