from clock import FakeClock
from gateway import TokenBucket, backoff_delay


def test_backoff_without_jitter_doubles():
    assert [backoff_delay(n, rng=lambda: 1.0) for n in range(4)] == [0.5, 1.0, 2.0, 4.0]


def test_bucket_starts_full():
    assert TokenBucket(100, 10, FakeClock()).available() == 100
