"""A tiny lakehouse table: Parquet files plus a transaction log of JSON commits."""

import json
import os
import uuid
from pathlib import Path

import pyarrow as pa
import pyarrow.compute as pc
import pyarrow.parquet as pq

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

    # ---- the log -------------------------------------------------------------------------

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
        latest = self.version()
        if read_version > latest:
            raise ValueError(f"version {read_version} does not exist (latest is {latest})")
        version = read_version + 1
        self.log.mkdir(parents=True, exist_ok=True)
        temporary = self.log / f".{uuid.uuid4().hex}.tmp"
        temporary.write_text(
            "".join(json.dumps(action, sort_keys=True) + "\n" for action in actions),
            encoding="utf-8",
        )
        try:
            # A hard link appears complete or not at all, and never replaces a file.
            os.link(temporary, self._log_path(version))
        except FileExistsError:
            raise CommitConflict(f"version {version} was committed by another writer") from None
        finally:
            temporary.unlink()
        return version

    def _replay(self, version: int | None) -> tuple[dict[str, dict], dict[str, dict]]:
        latest = self.version()
        if version is None:
            version = latest
        elif not 0 <= version <= latest:
            raise ValueError(f"version {version} does not exist (latest is {latest})")
        files: dict[str, dict] = {}
        txns: dict[str, dict] = {}
        for number in range(version + 1):
            text = self._log_path(number).read_text(encoding="utf-8")
            for line in text.splitlines():
                if not line.strip():
                    continue
                action = json.loads(line)
                if "add" in action:
                    files[action["add"]["path"]] = action["add"]
                elif "remove" in action:
                    files.pop(action["remove"]["path"], None)
                elif "txn" in action:
                    txns[action["txn"]["app_id"]] = action["txn"]["offsets"]
        return files, txns

    def files(self, version: int | None = None) -> list[dict]:
        """The data files of a snapshot, sorted by path."""
        files, _ = self._replay(version)
        return [dict(files[path]) for path in sorted(files)]

    def offsets(self, app_id: str, version: int | None = None) -> dict[int, int]:
        """The offsets the application committed last, as {partition: next offset}."""
        _, txns = self._replay(version)
        return {int(partition): offset for partition, offset in txns.get(app_id, {}).items()}

    # ---- writing -------------------------------------------------------------------------

    def _write_file(self, partition: str, rows: pa.Table) -> dict:
        path = f"{PARTITION}={partition}/{uuid.uuid4().hex}.parquet"
        (self.root / path).parent.mkdir(parents=True, exist_ok=True)
        pq.write_table(rows.drop_columns([PARTITION]), self.root / path)
        return {
            "path": path,
            "partition": partition,
            "rows": rows.num_rows,
            "min": pc.min(rows[STATS]).as_py(),
            "max": pc.max(rows[STATS]).as_py(),
        }

    def append(self, rows: pa.Table, txn: tuple[str, dict[int, int]] | None = None) -> int | None:
        """Add rows (one new file per partition) and the offsets in txn, in one commit."""
        read_version = self.version()
        if rows.num_rows == 0 and txn is None:
            return None
        rows = rows.select(SCHEMA.names).cast(SCHEMA)
        actions = []
        for partition in sorted(pc.unique(rows[PARTITION]).to_pylist()):
            part = rows.filter(pc.equal(rows[PARTITION], partition))
            actions.append({"add": self._write_file(partition, part)})
        if txn is not None:
            app_id, offsets = txn
            actions.append({"txn": {
                "app_id": app_id,
                "offsets": {str(partition): offset for partition, offset in offsets.items()},
            }})
        return self.commit(actions, read_version)

    def compact(self, partition: str) -> int | None:
        """Rewrite the files of one partition as a single file sorted by event_id."""
        read_version = self.version()
        old = [f for f in self.files(read_version) if f["partition"] == partition]
        if len(old) < 2:
            return None
        merged = pa.concat_tables([self._read(f) for f in old]).sort_by("event_id")
        actions = [{"remove": {"path": f["path"]}} for f in old]
        actions.append({"add": self._write_file(partition, merged)})
        return self.commit(actions, read_version)

    def vacuum(self, oldest_version: int) -> list[str]:
        """Delete data files that no snapshot from oldest_version on needs."""
        latest = self.version()
        if not 0 <= oldest_version <= latest:
            raise ValueError(f"version {oldest_version} does not exist (latest is {latest})")
        keep = {f["path"] for version in range(oldest_version, latest + 1)
                for f in self.files(version)}
        deleted = []
        for path in sorted(self.root.rglob("*.parquet")):
            relative = path.relative_to(self.root).as_posix()
            if relative not in keep:
                path.unlink()
                deleted.append(relative)
        return deleted

    # ---- reading -------------------------------------------------------------------------

    def _read(self, entry: dict) -> pa.Table:
        part = pq.read_table(self.root / entry["path"])
        part = part.append_column(
            PARTITION, pa.array([entry["partition"]] * part.num_rows, pa.string()))
        return part.select(SCHEMA.names).cast(SCHEMA)

    def scan(self, version: int | None = None, *, dates: tuple[str, str] | None = None,
             min_amount: int | None = None,
             columns: list[str] | None = None) -> tuple[pa.Table, list[str]]:
        """Rows of a snapshot sorted by event_id, and the paths of the files opened."""
        tables, opened = [], []
        for entry in self.files(version):
            if dates is not None and not dates[0] <= entry["partition"] <= dates[1]:
                continue  # partition pruning
            if min_amount is not None and entry["max"] < min_amount:
                continue  # file pruning by statistics
            tables.append(self._read(entry))
            opened.append(entry["path"])
        result = pa.concat_tables(tables) if tables else SCHEMA.empty_table()
        if min_amount is not None:
            result = result.filter(pc.greater_equal(result[STATS], min_amount))
        result = result.sort_by("event_id")
        if columns is not None:
            result = result.select(columns)
        return result, opened
