import pytest

from release import percentile, weights_gb


def test_weights_follow_precision():
    assert weights_gb(70, 4) == pytest.approx(35)


def test_percentile_is_nearest_rank():
    assert percentile([30, 10, 20], 95) == 30
