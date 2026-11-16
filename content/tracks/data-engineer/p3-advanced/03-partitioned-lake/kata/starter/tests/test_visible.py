import pyarrow as pa

from lake import partitions, read_range, write_partitioned

EVENTS = pa.table({
    "event_id": [1, 2, 3],
    "user_id": ["ada", "grace", "ada"],
    "event_date": ["2026-11-01", "2026-11-01", "2026-11-02"],
    "amount_cents": [500, 250, 990],
})


def test_one_folder_per_day(tmp_path):
    write_partitioned(EVENTS, tmp_path)
    assert partitions(tmp_path) == ["2026-11-01", "2026-11-02"]
    assert (tmp_path / "event_date=2026-11-02" / "part-0.parquet").is_file()


def test_reading_one_day_opens_one_file(tmp_path):
    write_partitioned(EVENTS, tmp_path)
    table, opened = read_range(tmp_path, "2026-11-02", "2026-11-02")
    assert table.column("event_id").to_pylist() == [3]
    assert len(opened) == 1
