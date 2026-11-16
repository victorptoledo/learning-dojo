from windows import Event, TumblingWindows, WindowResult


def test_a_window_closes_when_the_watermark_passes_its_end():
    windows = TumblingWindows(size=60)
    assert windows.process(Event(0, 0, "a", 10, 5)) == []
    assert windows.process(Event(0, 1, "a", 59, 1)) == []
    assert windows.process(Event(0, 2, "a", 60, 2)) == [WindowResult("a", 0, 60, 2, 6)]


def test_flush_emits_what_is_still_open():
    windows = TumblingWindows(size=60)
    windows.process(Event(0, 0, "a", 10, 5))
    assert windows.flush() == [WindowResult("a", 0, 60, 1, 5)]
    assert windows.flush() == []
