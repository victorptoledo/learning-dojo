"""A tiny lakehouse table: Parquet files plus a transaction log of JSON commits."""

from pathlib import Path

import pyarrow as pa

PARTITION = "event_date"
STATS = "amount_cents"
SCHEMA = pa.schema([
    ("event_id", pa.int64()),
    ("user_id", pa.string()),
    ("event_date", pa.string()),
    ("amount_cents", pa.int64()),
])


class CommitConflict(Exception):
    """Another writer committed this version first."""


class Table:
    def __init__(self, root):
        self.root = Path(root)
        self.log = self.root / "_log"

    def _log_path(self, version: int) -> Path:
        return self.log / f"{version:020d}.json"

    def version(self) -> int:
        """The latest committed version, -1 for a table without commits."""
        if not self.log.is_dir():
            return -1
        versions = [int(path.stem) for path in self.log.glob("*.json") if path.stem.isdigit()]
        return max(versions, default=-1)

    def commit(self, actions: list[dict], read_version: int) -> int:
        """Write version read_version + 1, or raise CommitConflict if it already exists."""
        raise NotImplementedError

    def files(self, version: int | None = None) -> list[dict]:
        """The data files of a snapshot, sorted by path."""
        raise NotImplementedError

    def offsets(self, app_id: str, version: int | None = None) -> dict[int, int]:
        """The offsets the application committed last, as {partition: next offset}."""
        raise NotImplementedError

    def append(self, rows: pa.Table, txn: tuple[str, dict[int, int]] | None = None) -> int | None:
        """Add rows (one new file per partition) and the offsets in txn, in one commit."""
        raise NotImplementedError

    def compact(self, partition: str) -> int | None:
        """Rewrite the files of one partition as a single file sorted by event_id."""
        raise NotImplementedError

    def vacuum(self, oldest_version: int) -> list[str]:
        """Delete data files that no snapshot from oldest_version on needs."""
        raise NotImplementedError

    def scan(self, version: int | None = None, *, dates: tuple[str, str] | None = None,
             min_amount: int | None = None,
             columns: list[str] | None = None) -> tuple[pa.Table, list[str]]:
        """Rows of a snapshot sorted by event_id, and the paths of the files opened."""
        raise NotImplementedError
