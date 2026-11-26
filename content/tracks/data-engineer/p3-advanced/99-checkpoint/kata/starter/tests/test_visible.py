import pyarrow as pa

from sink import run_batch
from table import SCHEMA, Table
from topic import Topic


def test_an_append_is_one_commit_with_one_file_per_partition(tmp_path):
    table = Table(tmp_path)
    rows = pa.Table.from_pylist([
        {"event_id": 1, "user_id": "ada", "event_date": "2026-11-01", "amount_cents": 500},
        {"event_id": 2, "user_id": "grace", "event_date": "2026-11-02", "amount_cents": 250},
        {"event_id": 3, "user_id": "ada", "event_date": "2026-11-01", "amount_cents": 990},
    ], schema=SCHEMA)
    assert table.append(rows) == 0
    assert [(f["partition"], f["rows"], f["min"], f["max"]) for f in table.files()] == [
        ("2026-11-01", 2, 500, 990), ("2026-11-02", 1, 250, 250)]
    assert table.scan()[0].to_pylist() == rows.sort_by("event_id").to_pylist()


def test_the_sink_stores_its_offsets_with_the_rows(tmp_path):
    topic = Topic(2)
    topic.produce(0, {"event_id": 1, "user_id": "ada",
                      "event_time": "2026-11-01T10:00:00+00:00", "amount_cents": 500})
    topic.produce(1, {"event_id": 2, "user_id": "grace",
                      "event_time": "2026-11-01T11:00:00+00:00", "amount_cents": 250})
    table = Table(tmp_path)
    assert run_batch(topic, table, "orders") == 2
    assert table.offsets("orders") == {0: 1, 1: 1}
    assert run_batch(topic, table, "orders") == 0
    assert table.scan()[0].num_rows == 2
