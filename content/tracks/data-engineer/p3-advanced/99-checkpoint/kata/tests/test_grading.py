import json

import pyarrow as pa
import pyarrow.parquet as pq
import pytest

from sink import event_date, lag, run_batch
from table import SCHEMA, CommitConflict, Table
from topic import Topic


def rows(*values):
    return pa.Table.from_pylist(
        [{"event_id": i, "user_id": u, "event_date": d, "amount_cents": a}
         for i, u, d, a in values],
        schema=SCHEMA)


NOV = rows(
    (1, "ada", "2026-11-01", 500),
    (2, "grace", "2026-11-02", 250),
    (3, "ada", "2026-11-01", 990),
    (4, "linus", "2026-11-03", 100),
)


def event(event_id, when="2026-11-01T10:00:00+00:00", amount=100, user="ada"):
    return {"event_id": event_id, "user_id": user, "event_time": when, "amount_cents": amount}


def ids(table, **kwargs):
    return table.scan(**kwargs)[0]["event_id"].to_pylist()


def log_actions(root, version):
    text = (root / "_log" / f"{version:020d}.json").read_text(encoding="utf-8")
    return [json.loads(line) for line in text.splitlines() if line.strip()]


# ---- commits -----------------------------------------------------------------------------

def test_a_new_table_is_empty(tmp_path):
    table = Table(tmp_path / "orders")
    assert table.version() == -1
    assert table.files() == []
    assert table.offsets("orders") == {}
    scanned, opened = table.scan()
    assert scanned.num_rows == 0 and scanned.schema.names == SCHEMA.names
    assert opened == []


def test_commits_are_numbered_log_files_of_json_lines(tmp_path):
    table = Table(tmp_path)
    assert table.commit([{"add": {"path": "a.parquet", "partition": "2026-11-01",
                                  "rows": 1, "min": 1, "max": 1}}], -1) == 0
    assert table.commit([{"remove": {"path": "a.parquet"}}], 0) == 1
    assert sorted(p.name for p in (tmp_path / "_log").iterdir()) == [
        "00000000000000000000.json", "00000000000000000001.json"]
    assert log_actions(tmp_path, 1) == [{"remove": {"path": "a.parquet"}}]
    assert table.version() == 1


def test_a_stale_writer_gets_a_conflict_and_the_winner_is_kept(tmp_path):
    first, second = Table(tmp_path), Table(tmp_path)
    first.append(NOV)
    read_version = second.version()
    first.append(rows((5, "ada", "2026-11-04", 10)))
    with pytest.raises(CommitConflict):
        second.commit([{"remove": {"path": first.files()[0]["path"]}}], read_version)
    assert ids(Table(tmp_path)) == [1, 2, 3, 4, 5]


def test_committing_after_a_version_that_does_not_exist_is_refused(tmp_path):
    table = Table(tmp_path)
    with pytest.raises(ValueError):
        table.commit([], 3)
    assert table.version() == -1


def test_data_files_without_a_commit_are_invisible(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    stray = tmp_path / "event_date=2026-11-09" / "crashed-writer.parquet"
    stray.parent.mkdir()
    pq.write_table(rows((9, "eve", "2026-11-09", 1)).drop_columns(["event_date"]), stray)
    assert ids(table) == [1, 2, 3, 4]
    assert all(f["partition"] != "2026-11-09" for f in table.files())


# ---- appends -----------------------------------------------------------------------------

def test_files_live_in_hive_style_folders_without_the_partition_column(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    for entry in table.files():
        assert entry["path"].startswith(f"event_date={entry['partition']}/")
        assert entry["path"].endswith(".parquet")
        stored = pq.read_table(tmp_path / entry["path"])
        assert "event_date" not in stored.column_names


def test_every_append_adds_new_files_and_keeps_the_old_ones(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    table.append(rows((5, "ada", "2026-11-01", 70)))
    assert [f["partition"] for f in table.files()].count("2026-11-01") == 2
    assert ids(table) == [1, 2, 3, 4, 5]
    assert len({f["path"] for f in table.files()}) == 4


def test_file_statistics_describe_amount_cents(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    stats = {f["partition"]: (f["rows"], f["min"], f["max"]) for f in table.files()}
    assert stats == {"2026-11-01": (2, 500, 990), "2026-11-02": (1, 250, 250),
                     "2026-11-03": (1, 100, 100)}


def test_an_empty_append_without_offsets_does_not_commit(tmp_path):
    table = Table(tmp_path)
    assert table.append(SCHEMA.empty_table()) is None
    assert table.version() == -1


def test_offsets_are_committed_with_the_rows_and_come_back_as_integers(tmp_path):
    table = Table(tmp_path)
    assert table.append(NOV, txn=("orders", {0: 3, 1: 2})) == 0
    assert table.offsets("orders") == {0: 3, 1: 2}
    assert table.offsets("someone-else") == {}
    assert table.append(SCHEMA.empty_table(), txn=("orders", {0: 4, 1: 2})) == 1
    assert table.offsets("orders") == {0: 4, 1: 2}
    assert table.offsets("orders", version=0) == {0: 3, 1: 2}


def test_each_application_keeps_its_own_offsets(tmp_path):
    table = Table(tmp_path)
    table.append(NOV, txn=("orders", {0: 3}))
    table.append(rows((5, "ada", "2026-11-04", 1)), txn=("refunds", {0: 1}))
    assert table.offsets("orders") == {0: 3}
    assert table.offsets("refunds") == {0: 1}


# ---- reading -----------------------------------------------------------------------------

def test_time_travel_reads_an_old_snapshot(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    table.append(rows((5, "ada", "2026-11-04", 10)))
    assert ids(table, version=0) == [1, 2, 3, 4]
    assert ids(table) == [1, 2, 3, 4, 5]
    with pytest.raises(ValueError):
        table.files(version=7)


def test_scan_returns_every_column_in_schema_order(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    scanned, _ = table.scan()
    assert scanned.schema == SCHEMA
    assert scanned.to_pylist()[0] == {"event_id": 1, "user_id": "ada",
                                      "event_date": "2026-11-01", "amount_cents": 500}


def test_scan_projects_columns(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    scanned, _ = table.scan(columns=["event_date", "event_id"])
    assert scanned.column_names == ["event_date", "event_id"]
    assert scanned.to_pylist()[-1] == {"event_date": "2026-11-03", "event_id": 4}


def test_partition_pruning_never_opens_other_dates(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    scanned, opened = table.scan(dates=("2026-11-02", "2026-11-03"))
    assert scanned["event_id"].to_pylist() == [2, 4]
    assert [p.split("/")[0] for p in opened] == ["event_date=2026-11-02",
                                                 "event_date=2026-11-03"]


def test_statistics_skip_files_whose_max_is_too_small(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    _, opened = table.scan(min_amount=600)
    assert [p.split("/")[0] for p in opened] == ["event_date=2026-11-01"]


def test_a_file_that_may_match_is_still_filtered_row_by_row(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    assert ids(table, min_amount=600) == [3]
    assert ids(table, min_amount=100, dates=("2026-11-03", "2026-11-03")) == [4]


def test_scan_works_from_a_fresh_object(tmp_path):
    Table(tmp_path).append(NOV)
    assert ids(Table(tmp_path)) == [1, 2, 3, 4]


# ---- maintenance -------------------------------------------------------------------------

def test_compaction_rewrites_one_partition_as_one_sorted_file(tmp_path):
    table = Table(tmp_path)
    table.append(rows((7, "ada", "2026-11-01", 5), (2, "ada", "2026-11-02", 1)))
    table.append(rows((3, "ada", "2026-11-01", 9)))
    table.append(rows((5, "ada", "2026-11-01", 1)))
    before = ids(table)
    assert table.compact("2026-11-01") == 3
    day = [f for f in table.files() if f["partition"] == "2026-11-01"]
    assert len(day) == 1 and (day[0]["rows"], day[0]["min"], day[0]["max"]) == (3, 1, 9)
    stored = pq.read_table(tmp_path / day[0]["path"])
    assert stored["event_id"].to_pylist() == [3, 5, 7]
    assert ids(table) == before
    assert len([f for f in table.files() if f["partition"] == "2026-11-02"]) == 1


def test_compaction_is_one_commit_of_removes_and_one_add(tmp_path):
    table = Table(tmp_path)
    table.append(rows((1, "ada", "2026-11-01", 5)))
    table.append(rows((2, "ada", "2026-11-01", 6)))
    old = {f["path"] for f in table.files()}
    version = table.compact("2026-11-01")
    actions = log_actions(tmp_path, version)
    assert {a["remove"]["path"] for a in actions if "remove" in a} == old
    assert len([a for a in actions if "add" in a]) == 1


def test_compacting_a_single_file_does_nothing(tmp_path):
    table = Table(tmp_path)
    table.append(NOV)
    assert table.compact("2026-11-02") is None
    assert table.compact("2030-01-01") is None
    assert table.version() == 0


def test_old_snapshots_still_read_after_compaction(tmp_path):
    table = Table(tmp_path)
    table.append(rows((1, "ada", "2026-11-01", 5)))
    table.append(rows((2, "ada", "2026-11-01", 6)))
    table.compact("2026-11-01")
    assert ids(table, version=0) == [1]
    assert ids(table, version=1) == [1, 2]


def test_vacuum_deletes_unreferenced_files_and_keeps_the_kept_snapshots(tmp_path):
    table = Table(tmp_path)
    table.append(rows((1, "ada", "2026-11-01", 5)))
    table.append(rows((2, "ada", "2026-11-01", 6)))
    v1_files = {f["path"] for f in table.files(version=1)}
    table.compact("2026-11-01")
    orphan = tmp_path / "event_date=2026-11-01" / "orphan.parquet"
    pq.write_table(pa.table({"x": [1]}), orphan)
    assert table.vacuum(2) == sorted([*v1_files, "event_date=2026-11-01/orphan.parquet"])
    assert ids(table) == [1, 2]
    assert table.vacuum(2) == []


def test_vacuum_keeps_files_of_every_snapshot_it_was_asked_to_keep(tmp_path):
    table = Table(tmp_path)
    table.append(rows((1, "ada", "2026-11-01", 5)))
    table.append(rows((2, "ada", "2026-11-01", 6)))
    table.compact("2026-11-01")
    assert table.vacuum(1) == []
    assert ids(table, version=1) == [1, 2]
    with pytest.raises(ValueError):
        table.vacuum(9)


# ---- the sink ----------------------------------------------------------------------------

def test_event_dates_are_utc_dates():
    assert event_date("2026-11-01T23:30:00+00:00") == "2026-11-01"
    assert event_date("2026-11-01T22:30:00-03:00") == "2026-11-02"
    assert event_date("2026-11-02T01:00:00+05:00") == "2026-11-01"
    assert event_date("2026-11-01T12:00:00Z") == "2026-11-01"


def test_late_events_land_in_the_partition_of_their_event_time(tmp_path):
    topic, table = Topic(1), Table(tmp_path)
    topic.produce(0, event(1, "2026-11-02T08:00:00+00:00"))
    topic.produce(0, event(2, "2026-11-01T23:59:00+00:00"))  # arrives late
    topic.produce(0, event(3, "2026-11-01T21:30:00-03:00"))  # 00:30 UTC on 2 November
    run_batch(topic, table, "orders")
    by_day = table.scan(columns=["event_date", "event_id"])[0].to_pylist()
    assert by_day == [{"event_date": "2026-11-02", "event_id": 1},
                      {"event_date": "2026-11-01", "event_id": 2},
                      {"event_date": "2026-11-02", "event_id": 3}]


def test_a_batch_is_capped_per_partition_and_resumes_from_the_table(tmp_path):
    topic, table = Topic(2), Table(tmp_path)
    for n in range(5):
        topic.produce(0, event(n))
    topic.produce(1, event(100))
    assert run_batch(topic, table, "orders", max_records=2) == 3
    assert table.offsets("orders") == {0: 2, 1: 1}
    assert run_batch(topic, Table(tmp_path), "orders", max_records=2) == 2
    assert run_batch(topic, table, "orders", max_records=2) == 1
    assert run_batch(topic, table, "orders", max_records=2) == 0
    assert ids(table) == [0, 1, 2, 3, 4, 100]
    assert table.version() == 2


def test_offsets_cover_every_partition_even_an_empty_one(tmp_path):
    topic, table = Topic(3), Table(tmp_path)
    topic.produce(1, event(1))
    run_batch(topic, table, "orders")
    assert table.offsets("orders") == {0: 0, 1: 1, 2: 0}


def test_a_crash_before_the_commit_loses_nothing_and_duplicates_nothing(tmp_path, monkeypatch):
    topic, table = Topic(2), Table(tmp_path)
    for n in range(3):
        topic.produce(n % 2, event(n))
    original = Table.commit

    def crash(self, actions, read_version):
        raise OSError("the writer died before committing")

    monkeypatch.setattr(Table, "commit", crash)
    with pytest.raises(OSError):
        run_batch(topic, table, "orders")
    monkeypatch.setattr(Table, "commit", original)
    assert ids(table) == []
    assert run_batch(topic, table, "orders") == 3
    assert ids(table) == [0, 1, 2]
    assert len(table.vacuum(table.version())) >= 1  # the crashed writer's files


def test_producer_retries_are_stored_once(tmp_path):
    topic, table = Topic(2), Table(tmp_path)
    topic.produce(0, event(1, amount=500))
    topic.produce(0, event(1, amount=500))
    assert run_batch(topic, table, "orders") == 1
    topic.produce(1, event(1, amount=500))
    topic.produce(1, event(2))
    assert run_batch(topic, table, "orders") == 1
    assert ids(table) == [1, 2]
    assert table.offsets("orders") == {0: 2, 1: 2}


def test_a_batch_of_only_duplicates_still_moves_the_offsets(tmp_path):
    topic, table = Topic(1), Table(tmp_path)
    topic.produce(0, event(1))
    run_batch(topic, table, "orders")
    topic.produce(0, event(1))
    assert run_batch(topic, table, "orders") == 0
    assert table.offsets("orders") == {0: 2}
    assert table.version() == 1
    assert run_batch(topic, table, "orders") == 0
    assert table.version() == 1


def test_lag_is_what_the_application_has_not_committed(tmp_path):
    topic, table = Topic(2), Table(tmp_path)
    for n in range(4):
        topic.produce(0, event(n))
    topic.produce(1, event(10))
    assert lag(topic, table, "orders") == {0: 4, 1: 1}
    run_batch(topic, table, "orders", max_records=3)
    assert lag(topic, table, "orders") == {0: 1, 1: 0}
    assert lag(topic, table, "audit") == {0: 4, 1: 1}
