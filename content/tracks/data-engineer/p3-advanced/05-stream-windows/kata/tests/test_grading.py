import pytest

from windows import Event, TumblingWindows, WindowResult


def feed(windows, events):
    results = []
    for event in events:
        results.extend(windows.process(event))
    return results


def test_size_and_lateness_are_checked():
    for size in (0, -60):
        with pytest.raises(ValueError):
            TumblingWindows(size=size)
    with pytest.raises(ValueError):
        TumblingWindows(size=60, allowed_lateness=-1)


def test_windows_are_aligned_to_multiples_of_the_size():
    windows = TumblingWindows(size=60)
    windows.process(Event(0, 0, "a", 125, 1))
    assert windows.flush() == [WindowResult("a", 120, 180, 1, 1)]


def test_negative_times_are_floored():
    windows = TumblingWindows(size=60)
    windows.process(Event(0, 0, "a", -1, 1))
    assert windows.flush() == [WindowResult("a", -60, 0, 1, 1)]


def test_the_watermark_follows_the_latest_event_time_minus_the_lateness():
    windows = TumblingWindows(size=60, allowed_lateness=10)
    assert windows.watermark is None
    windows.process(Event(0, 0, "a", 100, 1))
    assert windows.watermark == 90
    windows.process(Event(0, 1, "a", 95, 1))
    assert windows.watermark == 90
    windows.process(Event(1, 0, "a", 130, 1))
    assert windows.watermark == 120


def test_out_of_order_events_within_the_lateness_still_count():
    windows = TumblingWindows(size=60, allowed_lateness=10)
    results = feed(windows, [
        Event(0, 0, "a", 5, 1),
        Event(0, 1, "b", 30, 2),
        Event(1, 0, "a", 65, 3),
        Event(1, 1, "a", 55, 10),  # out of order, but window [0, 60) is still open
    ])
    assert results == []
    assert feed(windows, [Event(1, 2, "a", 75, 1)]) == [
        WindowResult("a", 0, 60, 2, 11),
        WindowResult("b", 0, 60, 1, 2),
    ]


def test_events_for_a_closed_window_are_late_and_set_aside():
    windows = TumblingWindows(size=60, allowed_lateness=10)
    feed(windows, [Event(0, 0, "a", 5, 1), Event(0, 1, "a", 75, 1)])
    late = Event(0, 2, "a", 50, 4)
    assert windows.process(late) == []
    assert windows.late == [late]
    assert windows.flush() == [WindowResult("a", 60, 120, 1, 1)]


def test_a_window_ending_exactly_at_the_watermark_is_closed():
    windows = TumblingWindows(size=60)
    assert feed(windows, [Event(0, 0, "a", 0, 1), Event(0, 1, "a", 60, 1)]) == [
        WindowResult("a", 0, 60, 1, 1)
    ]
    assert windows.process(Event(0, 2, "a", 59, 1)) == []
    assert len(windows.late) == 1


def test_a_late_event_does_not_move_the_watermark():
    windows = TumblingWindows(size=60)
    feed(windows, [Event(0, 0, "a", 200, 1), Event(0, 1, "a", 10, 1)])
    assert windows.watermark == 200


def test_one_event_can_close_several_windows_in_time_then_key_order():
    windows = TumblingWindows(size=10, allowed_lateness=10)
    feed(windows, [Event(0, 0, "b", 1, 1), Event(0, 1, "a", 2, 1), Event(0, 2, "c", 15, 1),
                   Event(0, 3, "a", 18, 1)])
    assert windows.process(Event(0, 4, "z", 45, 1)) == [
        WindowResult("a", 0, 10, 1, 1),
        WindowResult("b", 0, 10, 1, 1),
        WindowResult("a", 10, 20, 1, 1),
        WindowResult("c", 10, 20, 1, 1),
    ]


def test_each_window_is_emitted_once():
    windows = TumblingWindows(size=60)
    first = feed(windows, [Event(0, 0, "a", 1, 1), Event(0, 1, "a", 61, 1)])
    second = feed(windows, [Event(0, 2, "a", 130, 1)])
    third = windows.flush()
    starts = [r.start for r in first + second + third]
    assert starts == [0, 60, 120]


def test_redelivered_offsets_are_ignored():
    windows = TumblingWindows(size=60)
    events = [Event(0, 0, "a", 1, 5), Event(0, 1, "a", 2, 5), Event(1, 0, "a", 3, 5)]
    feed(windows, events)
    feed(windows, events)  # the consumer restarted before committing
    feed(windows, [Event(0, 1, "a", 2, 5)])
    assert windows.flush() == [WindowResult("a", 0, 60, 3, 15)]


def test_offsets_are_tracked_per_partition():
    windows = TumblingWindows(size=60)
    feed(windows, [Event(0, 7, "a", 1, 1), Event(1, 3, "a", 1, 1), Event(1, 2, "a", 1, 1)])
    assert windows.flush() == [WindowResult("a", 0, 60, 2, 2)]


def test_next_offsets_to_commit():
    windows = TumblingWindows(size=60)
    assert windows.next_offsets() == {}
    feed(windows, [Event(0, 0, "a", 1, 1), Event(0, 1, "a", 2, 1), Event(2, 9, "b", 3, 1)])
    assert windows.next_offsets() == {0: 2, 2: 10}


def test_late_events_are_still_consumed():
    windows = TumblingWindows(size=60)
    feed(windows, [Event(0, 0, "a", 100, 1), Event(0, 1, "a", 1, 1)])
    assert windows.next_offsets() == {0: 2}
    windows.process(Event(0, 1, "a", 1, 1))
    assert len(windows.late) == 1


def test_a_whole_stream():
    windows = TumblingWindows(size=60, allowed_lateness=30)
    stream = [
        Event(0, 0, "checkout", 10, 100),
        Event(1, 0, "checkout", 70, 50),
        Event(0, 1, "refund", 40, -20),
        Event(0, 2, "checkout", 50, 5),
        Event(1, 1, "checkout", 95, 10),
        Event(1, 2, "checkout", 130, 1),
        Event(0, 3, "checkout", 55, 7),
    ]
    results = feed(windows, stream) + windows.flush()
    assert results == [
        WindowResult("checkout", 0, 60, 2, 105),
        WindowResult("refund", 0, 60, 1, -20),
        WindowResult("checkout", 60, 120, 2, 60),
        WindowResult("checkout", 120, 180, 1, 1),
    ]
    assert windows.late == [Event(0, 3, "checkout", 55, 7)]
