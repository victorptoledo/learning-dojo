import pyarrow as pa
import pyarrow.parquet as pq

from lake import (
    compact,
    daily_totals,
    partitions,
    read_range,
    row_groups_matching,
    write_partitioned,
)


def events(*rows):
    ids, users, days, amounts = zip(*rows, strict=True) if rows else ((), (), (), ())
    return pa.table({
        "event_id": pa.array(ids, pa.int64()),
        "user_id": pa.array(users, pa.string()),
        "event_date": pa.array(days, pa.string()),
        "amount_cents": pa.array(amounts, pa.int64()),
    })


NOV = events(
    (1, "ada", "2026-11-01", 500),
    (2, "grace", "2026-11-02", 250),
    (3, "ada", "2026-11-01", 990),
    (4, "linus", "2026-11-03", 100),
    (5, "grace", "2026-11-03", 50),
)


def files(root):
    return sorted(p.relative_to(root).as_posix() for p in root.rglob("*") if p.is_file())


def test_write_returns_the_files_in_partition_order(tmp_path):
    written = write_partitioned(NOV, tmp_path)
    assert [p.relative_to(tmp_path).as_posix() for p in written] == [
        "event_date=2026-11-01/part-0.parquet",
        "event_date=2026-11-02/part-0.parquet",
        "event_date=2026-11-03/part-0.parquet",
    ]


def test_the_partition_column_lives_in_the_path_only(tmp_path):
    write_partitioned(NOV, tmp_path)
    stored = pq.read_table(tmp_path / "event_date=2026-11-01" / "part-0.parquet")
    assert stored.column_names == ["event_id", "user_id", "amount_cents"]
    assert stored.column("event_id").to_pylist() == [1, 3]


def test_another_partition_column(tmp_path):
    write_partitioned(NOV, tmp_path, "user_id")
    assert partitions(tmp_path, "user_id") == ["ada", "grace", "linus"]
    assert partitions(tmp_path) == []


def test_writing_a_day_again_replaces_only_that_day(tmp_path):
    write_partitioned(NOV, tmp_path)
    pq.write_table(events((9, "x", "2026-11-02", 1)).drop_columns(["event_date"]),
                   tmp_path / "event_date=2026-11-02" / "part-1.parquet")
    write_partitioned(events((2, "grace", "2026-11-02", 300)), tmp_path)
    assert files(tmp_path) == [
        "event_date=2026-11-01/part-0.parquet",
        "event_date=2026-11-02/part-0.parquet",
        "event_date=2026-11-03/part-0.parquet",
    ]
    table, _ = read_range(tmp_path, "2026-11-02", "2026-11-02")
    assert table.column("amount_cents").to_pylist() == [300]


def test_writing_twice_is_idempotent(tmp_path):
    write_partitioned(NOV, tmp_path)
    write_partitioned(NOV, tmp_path)
    table, _ = read_range(tmp_path, "2026-11-01", "2026-11-30")
    assert table.num_rows == 5


def test_partitions_of_a_missing_or_mixed_folder(tmp_path):
    assert partitions(tmp_path / "nothing") == []
    write_partitioned(NOV, tmp_path)
    (tmp_path / "_SUCCESS").write_text("")
    (tmp_path / "tmp").mkdir()
    assert partitions(tmp_path) == ["2026-11-01", "2026-11-02", "2026-11-03"]


def test_read_range_prunes_partitions(tmp_path):
    write_partitioned(NOV, tmp_path)
    table, opened = read_range(tmp_path, "2026-11-02", "2026-11-03")
    assert [p.parent.name for p in opened] == ["event_date=2026-11-02", "event_date=2026-11-03"]
    assert table.column("event_id").to_pylist() == [2, 4, 5]


def test_read_range_puts_the_partition_column_back(tmp_path):
    write_partitioned(NOV, tmp_path)
    table, _ = read_range(tmp_path, "2026-11-01", "2026-11-01")
    assert table.column_names == ["event_id", "user_id", "amount_cents", "event_date"]
    assert table.column("event_date").to_pylist() == ["2026-11-01", "2026-11-01"]
    assert table.schema.field("event_date").type == pa.string()


def test_read_range_reads_only_the_columns_asked(tmp_path):
    write_partitioned(NOV, tmp_path)
    table, _ = read_range(tmp_path, "2026-11-01", "2026-11-03",
                          columns=["event_date", "amount_cents"])
    assert table.column_names == ["event_date", "amount_cents"]
    assert table.to_pylist()[0] == {"event_date": "2026-11-01", "amount_cents": 500}


def test_read_range_reads_every_file_of_a_partition(tmp_path):
    write_partitioned(NOV, tmp_path)
    pq.write_table(events((6, "ada", "2026-11-03", 7)).drop_columns(["event_date"]),
                   tmp_path / "event_date=2026-11-03" / "part-1.parquet")
    table, opened = read_range(tmp_path, "2026-11-03", "2026-11-03")
    assert [p.name for p in opened] == ["part-0.parquet", "part-1.parquet"]
    assert table.column("event_id").to_pylist() == [4, 5, 6]


def test_an_empty_range(tmp_path):
    write_partitioned(NOV, tmp_path)
    table, opened = read_range(tmp_path, "2026-12-01", "2026-12-31")
    assert table.num_rows == 0
    assert opened == []


def test_row_group_statistics_skip_groups(tmp_path):
    path = tmp_path / "sorted.parquet"
    pq.write_table(pa.table({"event_id": list(range(1, 11))}), path, row_group_size=3)
    assert pq.ParquetFile(path).metadata.num_row_groups == 4
    assert row_groups_matching(path, "event_id", 5, 6) == [1]
    assert row_groups_matching(path, "event_id", 5, 7) == [1, 2]
    assert row_groups_matching(path, "event_id", 10, 99) == [3]
    assert row_groups_matching(path, "event_id", 20, 30) == []
    assert row_groups_matching(path, "event_id", 3, 3) == [0]


def test_row_group_statistics_of_another_column(tmp_path):
    path = tmp_path / "two.parquet"
    pq.write_table(pa.table({"id": [1, 2, 3, 4], "amount": [50, 10, 70, 90]}), path,
                   row_group_size=2)
    assert row_groups_matching(path, "amount", 60, 80) == [1]
    assert row_groups_matching(path, "amount", 0, 20) == [0]


def test_without_statistics_every_group_must_be_read(tmp_path):
    path = tmp_path / "nostats.parquet"
    pq.write_table(pa.table({"event_id": list(range(10))}), path, row_group_size=5,
                   write_statistics=False)
    assert row_groups_matching(path, "event_id", 100, 200) == [0, 1]


def test_write_uses_the_row_group_size(tmp_path):
    many = events(*[(i, "u", "2026-11-01", i) for i in range(10)])
    [path] = write_partitioned(many, tmp_path, row_group_size=4)
    assert pq.ParquetFile(path).metadata.num_row_groups == 3


def test_compact_merges_small_files(tmp_path):
    write_partitioned(NOV, tmp_path)
    folder = tmp_path / "event_date=2026-11-03"
    for n in (1, 2):
        pq.write_table(events((10 + n, "ada", "2026-11-03", n)).drop_columns(["event_date"]),
                       folder / f"part-{n}.parquet")
    target = compact(tmp_path, "2026-11-03")
    assert target == folder / "part-0.parquet"
    assert files(folder) == ["part-0.parquet"]
    assert pq.read_table(target).column("event_id").to_pylist() == [4, 5, 11, 12]
    assert files(tmp_path / "event_date=2026-11-01") == ["part-0.parquet"]


def test_compacting_one_file_keeps_it(tmp_path):
    write_partitioned(NOV, tmp_path)
    target = compact(tmp_path, "2026-11-01")
    assert pq.read_table(target).column("event_id").to_pylist() == [1, 3]


def test_duckdb_reads_the_lake(tmp_path):
    write_partitioned(NOV, tmp_path)
    assert daily_totals(tmp_path) == [
        ("2026-11-01", 2, 1490),
        ("2026-11-02", 1, 250),
        ("2026-11-03", 2, 150),
    ]


def test_duckdb_sees_rewritten_partitions(tmp_path):
    write_partitioned(NOV, tmp_path)
    write_partitioned(events((7, "ada", "2026-11-02", 1000), (8, "ada", "2026-11-04", 1)),
                      tmp_path)
    assert daily_totals(tmp_path) == [
        ("2026-11-01", 2, 1490),
        ("2026-11-02", 1, 1000),
        ("2026-11-03", 2, 150),
        ("2026-11-04", 1, 1),
    ]
