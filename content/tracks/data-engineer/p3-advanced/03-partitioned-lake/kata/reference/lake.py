"""A small data lake: Hive-style partitioned Parquet files, pruning, compaction and DuckDB."""

from pathlib import Path

import duckdb
import pyarrow as pa
import pyarrow.compute as pc
import pyarrow.parquet as pq

PART = "part-0.parquet"


def partition_dir(root: Path, column: str, value: str) -> Path:
    return root / f"{column}={value}"


def write_partitioned(table: pa.Table, root: Path, column: str = "event_date", *,
                      row_group_size: int = 1000) -> list[Path]:
    """Write one file per value of `column`; a partition written again is replaced."""
    written = []
    for value in sorted(pc.unique(table[column]).to_pylist()):
        rows = table.filter(pc.equal(table[column], value)).drop_columns([column])
        folder = partition_dir(root, column, value)
        folder.mkdir(parents=True, exist_ok=True)
        for old in folder.glob("*.parquet"):
            old.unlink()
        path = folder / PART
        pq.write_table(rows, path, row_group_size=row_group_size)
        written.append(path)
    return written


def partitions(root: Path, column: str = "event_date") -> list[str]:
    """The partition values present under root, sorted."""
    prefix = f"{column}="
    if not root.is_dir():
        return []
    return sorted(
        entry.name.removeprefix(prefix)
        for entry in root.iterdir()
        if entry.is_dir() and entry.name.startswith(prefix)
    )


def read_range(root: Path, start: str, end: str, columns: list[str] | None = None,
               column: str = "event_date") -> tuple[pa.Table, list[Path]]:
    """Rows of the partitions from start to end (inclusive), and the files actually opened."""
    tables, opened = [], []
    for value in partitions(root, column):
        if not start <= value <= end:
            continue  # partition pruning: the file is never opened
        for path in sorted(partition_dir(root, column, value).glob("*.parquet")):
            wanted = None if columns is None else [c for c in columns if c != column]
            part = pq.read_table(path, columns=wanted)
            part = part.append_column(column, pa.array([value] * part.num_rows, pa.string()))
            if columns is not None:
                part = part.select(columns)
            tables.append(part)
            opened.append(path)
    if not tables:
        return pa.table({}), []
    return pa.concat_tables(tables), opened


def row_groups_matching(path: Path, column: str, low, high) -> list[int]:
    """Row groups whose statistics say they may hold a value from low to high."""
    metadata = pq.ParquetFile(path).metadata
    index = metadata.schema.to_arrow_schema().get_field_index(column)
    matching = []
    for group in range(metadata.num_row_groups):
        stats = metadata.row_group(group).column(index).statistics
        if stats is None or not stats.has_min_max:
            matching.append(group)  # nothing known: it must be read
        elif stats.max >= low and stats.min <= high:
            matching.append(group)
    return matching


def compact(root: Path, value: str, column: str = "event_date") -> Path:
    """Merge the files of one partition into a single file, in file name order."""
    folder = partition_dir(root, column, value)
    files = sorted(folder.glob("*.parquet"))
    merged = pa.concat_tables([pq.read_table(path) for path in files])
    temporary = folder / "compacting.tmp"
    pq.write_table(merged, temporary)
    for path in files:
        path.unlink()
    target = folder / PART
    temporary.rename(target)
    return target


def daily_totals(root: Path) -> list[tuple[str, int, int]]:
    """(event_date, events, amount_cents) per day, with DuckDB reading the lake."""
    pattern = (root / "*" / "*.parquet").as_posix()
    with duckdb.connect() as conn:
        return conn.execute(
            """SELECT CAST(event_date AS VARCHAR) AS day, count(*), sum(amount_cents)
               FROM read_parquet(?, hive_partitioning = true)
               GROUP BY day ORDER BY day""",
            [pattern],
        ).fetchall()
