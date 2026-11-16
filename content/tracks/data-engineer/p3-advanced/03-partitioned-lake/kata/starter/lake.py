"""A small data lake: Hive-style partitioned Parquet files, pruning, compaction and DuckDB."""

from pathlib import Path

import pyarrow as pa


def write_partitioned(table: pa.Table, root: Path, column: str = "event_date", *,
                      row_group_size: int = 1000) -> list[Path]:
    """Write one file per value of `column`; a partition written again is replaced."""
    raise NotImplementedError("todo: write write_partitioned()")


def partitions(root: Path, column: str = "event_date") -> list[str]:
    """The partition values present under root, sorted."""
    raise NotImplementedError("todo: write partitions()")


def read_range(root: Path, start: str, end: str, columns: list[str] | None = None,
               column: str = "event_date") -> tuple[pa.Table, list[Path]]:
    """Rows of the partitions from start to end (inclusive), and the files actually opened."""
    raise NotImplementedError("todo: write read_range()")


def row_groups_matching(path: Path, column: str, low, high) -> list[int]:
    """Row groups whose statistics say they may hold a value from low to high."""
    raise NotImplementedError("todo: write row_groups_matching()")


def compact(root: Path, value: str, column: str = "event_date") -> Path:
    """Merge the files of one partition into a single file, in file name order."""
    raise NotImplementedError("todo: write compact()")


def daily_totals(root: Path) -> list[tuple[str, int, int]]:
    """(event_date, events, amount_cents) per day, with DuckDB reading the lake."""
    raise NotImplementedError("todo: write daily_totals()")
