"""An exactly-once sink from a topic into the lakehouse table."""

from datetime import UTC, datetime

import pyarrow as pa

from table import SCHEMA, Table
from topic import Topic


def event_date(event_time: str) -> str:
    """The UTC date of an ISO 8601 timestamp with an offset."""
    return datetime.fromisoformat(event_time).astimezone(UTC).date().isoformat()


def run_batch(topic: Topic, table: Table, app_id: str, max_records: int = 100) -> int:
    """Move one micro-batch from the topic to the table and return the rows written."""
    committed = table.offsets(app_id)
    seen = set(table.scan(columns=["event_id"])[0]["event_id"].to_pylist())
    offsets, rows, moved = {}, [], False
    for partition in range(topic.partitions):
        offsets[partition] = committed.get(partition, 0)
        for offset, event in topic.read(partition, offsets[partition], max_records):
            moved = True
            offsets[partition] = offset + 1
            if event["event_id"] in seen:
                continue  # a producer retry, or a record we already stored
            seen.add(event["event_id"])
            rows.append({
                "event_id": event["event_id"],
                "user_id": event["user_id"],
                "event_date": event_date(event["event_time"]),
                "amount_cents": event["amount_cents"],
            })
    if not moved:
        return 0
    table.append(pa.Table.from_pylist(rows, schema=SCHEMA), txn=(app_id, offsets))
    return len(rows)


def lag(topic: Topic, table: Table, app_id: str) -> dict[int, int]:
    """Records per partition that the application has not committed yet."""
    committed = table.offsets(app_id)
    return {partition: topic.end_offset(partition) - committed.get(partition, 0)
            for partition in range(topic.partitions)}
