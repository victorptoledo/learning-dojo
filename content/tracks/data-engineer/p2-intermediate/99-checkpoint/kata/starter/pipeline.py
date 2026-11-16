"""The daily orders pipeline, as a DAG of idempotent tasks."""

from dag import DAG
from sources import Source, Warehouse


def build_dag(source: Source, warehouse: Warehouse) -> DAG:
    """The five tasks of the README, wired together."""
    raise NotImplementedError("todo: write build_dag()")
