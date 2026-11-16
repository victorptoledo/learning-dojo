# Event-Time Windows on a Stream

A consumer reads click events from a Kafka-like topic and counts them per key in
**tumbling windows** of `size` seconds by **event time** (when the event happened, not when
it arrived). Events arrive out of order, some arrive too late, and after a restart the
consumer receives again records it already counted. Write `TumblingWindows` in
`windows.py`; you may add fields and helper methods.

## Windows

The window of an event starts at `event_time` rounded **down** to a multiple of `size`
(`125` with `size=60` is in `[120, 180)`; `-1` is in `[-60, 0)`) and ends `size` later
(exclusive). A window holds, per key, the `count` of events and the `total` of their values.

## The watermark

`watermark` is `None` before the first event, then the **largest event time seen** minus
`allowed_lateness`. It says "no event older than this is expected any more". It never goes
back.

`process(event)` returns the windows that are **closed** after this event, as
`WindowResult`s ordered by `start`, then `key`. A window is closed when its `end` is less
than or equal to the watermark; each window is emitted **once**. `flush()` (the end of the
stream) emits every window still open, in the same order.

An event whose window was **already closed** when it arrived (its end <= the watermark
before this event) is **late**: it is not counted, does not move the watermark, and is
appended to `late` so that it can go to a dead-letter topic.

## Offsets

Each record has a `partition` and an `offset` that grows by 1 in its partition. After a
crash, the consumer starts again from its last **committed** offset and receives records it
has already processed. Kafka keeps the order inside a partition, so an event whose offset is
**not greater** than the last one processed in its partition is a redelivery: ignore it
(late events are processed too: they are not redeliveries).

`next_offsets()` returns, per partition seen, the offset to commit: the **next** one to read
(last processed + 1), as Kafka expects.

`size` must be greater than 0 and `allowed_lateness` 0 or more; anything else is a
`ValueError`.

Run the visible tests with `uv run pytest`. There are 18 tests: 2 in `tests/test_visible.py`
and 15 hidden ones, plus ruff on `windows.py`.
