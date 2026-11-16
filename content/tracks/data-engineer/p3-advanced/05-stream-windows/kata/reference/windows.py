"""Event-time tumbling windows over a Kafka-like stream, with a watermark and late events."""

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Event:
    partition: int
    offset: int  # position in its partition; grows by 1 per record
    key: str
    event_time: int  # seconds, when the event happened
    value: int


@dataclass(frozen=True)
class WindowResult:
    key: str
    start: int  # inclusive
    end: int  # exclusive
    count: int
    total: int


@dataclass
class Accumulator:
    count: int = 0
    total: int = 0


@dataclass
class TumblingWindows:
    size: int
    allowed_lateness: int = 0
    late: list[Event] = field(default_factory=list)
    _open: dict[tuple[int, str], Accumulator] = field(default_factory=dict)
    _last_offset: dict[int, int] = field(default_factory=dict)
    _max_event_time: int | None = None

    def __post_init__(self):
        if self.size <= 0:
            raise ValueError("size must be positive")
        if self.allowed_lateness < 0:
            raise ValueError("allowed_lateness cannot be negative")

    @property
    def watermark(self) -> int | None:
        """No window ending at or before this time can still receive events."""
        if self._max_event_time is None:
            return None
        return self._max_event_time - self.allowed_lateness

    def next_offsets(self) -> dict[int, int]:
        """Per partition, the offset to commit: the next one to read."""
        return {partition: offset + 1 for partition, offset in self._last_offset.items()}

    def process(self, event: Event) -> list[WindowResult]:
        """Add one event; returns the windows that the new watermark closes."""
        last = self._last_offset.get(event.partition)
        if last is not None and event.offset <= last:
            return []  # redelivered after a restart: already counted
        self._last_offset[event.partition] = event.offset

        start = event.event_time - event.event_time % self.size
        watermark = self.watermark
        if watermark is not None and start + self.size <= watermark:
            self.late.append(event)
            return []
        accumulator = self._open.setdefault((start, event.key), Accumulator())
        accumulator.count += 1
        accumulator.total += event.value

        if self._max_event_time is None or event.event_time > self._max_event_time:
            self._max_event_time = event.event_time
        return self._emit(lambda window_start: window_start + self.size <= self.watermark)

    def flush(self) -> list[WindowResult]:
        """End of the stream: emit every window still open."""
        return self._emit(lambda window_start: True)

    def _emit(self, closed) -> list[WindowResult]:
        ready = sorted(key for key in self._open if closed(key[0]))
        results = []
        for start, key in ready:
            accumulator = self._open.pop((start, key))
            results.append(
                WindowResult(key, start, start + self.size, accumulator.count, accumulator.total)
            )
        return results
