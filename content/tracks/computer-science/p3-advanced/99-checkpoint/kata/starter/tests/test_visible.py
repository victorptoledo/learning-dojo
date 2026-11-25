from checker import enabled, explore, initial_state, step

INCREMENT = [("load", "r", "x"), ("add", "r", 1), ("store", "x", "r")]


def test_one_step_of_one_thread():
    threads = [INCREMENT, INCREMENT]
    start = initial_state(threads, {"x": 0})
    after = step(threads, start, 1)
    assert after.pcs == (0, 1)
    assert after.register(1, "r") == 0 and start.pcs == (0, 0)
    assert enabled(threads, after) == [0, 1]


def test_count_plus_plus_loses_updates():
    report = explore([INCREMENT, INCREMENT], {"x": 0})
    assert report.final_values("x") == {1, 2}
    assert report.deadlock is None
