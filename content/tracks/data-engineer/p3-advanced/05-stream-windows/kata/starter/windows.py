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
class TumblingWindows:
    size: int
    allowed_lateness: int = 0
    late: list[Event] = field(default_factory=list)

    def __post_init__(self):
        raise NotImplementedError("todo: validate size and allowed_lateness, set up the state")

    @property
    def watermark(self) -> int | None:
        """No window ending at or before this time can still receive events."""
        raise NotImplementedError("todo: write watermark")

    def next_offsets(self) -> dict[int, int]:
        """Per partition, the offset to commit: the next one to read."""
        raise NotImplementedError("todo: write next_offsets()")

    def process(self, event: Event) -> list[WindowResult]:
        """Add one event; returns the windows that the new watermark closes."""
        raise NotImplementedError("todo: write process()")

    def flush(self) -> list[WindowResult]:
        """End of the stream: emit every window still open."""
        raise NotImplementedError("todo: write flush()")
