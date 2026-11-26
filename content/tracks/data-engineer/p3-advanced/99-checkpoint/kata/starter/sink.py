"""An exactly-once sink from a topic into the lakehouse table."""

from table import Table
from topic import Topic


def event_date(event_time: str) -> str:
    """The UTC date of an ISO 8601 timestamp with an offset."""
    raise NotImplementedError


def run_batch(topic: Topic, table: Table, app_id: str, max_records: int = 100) -> int:
    """Move one micro-batch from the topic to the table and return the rows written."""
    raise NotImplementedError


def lag(topic: Topic, table: Table, app_id: str) -> dict[int, int]:
    """Records per partition that the application has not committed yet."""
    raise NotImplementedError
