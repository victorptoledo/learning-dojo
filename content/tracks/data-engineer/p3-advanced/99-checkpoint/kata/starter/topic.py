"""A tiny stand-in for a Kafka topic: partitions of append-only records with offsets.

Given; do not change it.
"""


class Topic:
    """Partitions of records. A record's offset is its position in its partition."""

    def __init__(self, partitions: int):
        if partitions < 1:
            raise ValueError("a topic needs at least one partition")
        self._logs: list[list[dict]] = [[] for _ in range(partitions)]

    @property
    def partitions(self) -> int:
        return len(self._logs)

    def produce(self, partition: int, event: dict) -> int:
        """Append a copy of event to the partition and return its offset."""
        self._logs[partition].append(dict(event))
        return len(self._logs[partition]) - 1

    def read(self, partition: int, offset: int, max_records: int) -> list[tuple[int, dict]]:
        """Up to max_records (offset, event) pairs, starting at offset."""
        log = self._logs[partition]
        end = min(len(log), offset + max_records)
        return [(position, dict(log[position])) for position in range(offset, end)]

    def end_offset(self, partition: int) -> int:
        """The offset the next record of the partition will get."""
        return len(self._logs[partition])
