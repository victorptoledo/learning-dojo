# A Partitioned Parquet Lake

Lakes and lakehouses store tables as Parquet files in folders, one folder per partition,
named the Hive way: `events/event_date=2026-11-01/part-0.parquet`. Readers skip whole
folders that cannot match a filter (**partition pruning**) and, inside a file, whole row
groups whose min/max **statistics** cannot match. Build the pieces in `lake.py` with
**pyarrow**, and let **DuckDB** query the result. This kata runs in the `dojo-data` image,
which has both installed.

Rows are `pyarrow.Table`s; partition values are strings (ISO dates compare correctly as
text).

| Function | Does |
| --- | --- |
| `write_partitioned(table, root, column="event_date", *, row_group_size=1000)` | writes one file `root/column=value/part-0.parquet` per distinct value, in sorted value order, and returns their paths in that order. The partition column is **not** stored in the file: it lives in the folder name. Writing a value again **replaces** that partition (every `.parquet` file in its folder) and leaves the others alone, so a rerun is idempotent. Pass `row_group_size` to `pyarrow.parquet.write_table` |
| `partitions(root, column="event_date")` | the sorted values of the `column=value` folders under `root` (other files and folders are ignored; a missing `root` has none) |
| `read_range(root, start, end, columns=None, column="event_date")` | `(table, opened)`: the rows of the partitions from `start` to `end` inclusive, and the list of files it opened. Never open a file of another partition. Read every `.parquet` file of a partition, in file name order. Put the partition column back as a string column, **last** when `columns` is `None`; with `columns`, read only those (the partition column included if asked) in that order. No matching partition gives an empty table and `[]` |
| `row_groups_matching(path, column, low, high)` | the indexes of the row groups of one file that **may** hold a value of `column` from `low` to `high`, judged by the row group statistics alone. A row group without statistics must be read, so it always matches |
| `compact(root, value, column="event_date")` | merges all files of one partition (in file name order) into one `part-0.parquet` and returns its path. Write the merged file under a temporary name first, then delete the old files and rename it, so a crash never leaves the partition without its data |
| `daily_totals(root)` | `[(event_date, events, amount_cents)]` per day in date order, computed by **DuckDB** with `read_parquet(..., hive_partitioning = true)` over `root/*/*.parquet`. DuckDB reads `event_date` back as a `DATE`: cast it to text |

> [!TIP]
> `pyarrow.compute.unique`, `Table.filter`, `Table.drop_columns`, `Table.append_column`,
> `Table.select` and `pa.concat_tables` do most of the work. Statistics are in
> `pq.ParquetFile(path).metadata.row_group(i).column(j).statistics` (`has_min_max`, `min`,
> `max`).

To run the visible tests on your machine you need pyarrow and DuckDB: `uv run pytest`
installs them from `pyproject.toml`. There are 22 tests: 2 in `tests/test_visible.py` and
19 hidden ones, plus ruff on `lake.py`.
