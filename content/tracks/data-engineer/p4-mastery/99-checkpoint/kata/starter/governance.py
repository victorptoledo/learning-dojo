"""Lineage, personal data and access control over the catalog."""

from catalog import Grant, Model


class Lineage:
    def __init__(self, models):
        self.models: dict[str, Model] = {}
        raise NotImplementedError

    def order(self) -> list[str]:
        raise NotImplementedError

    def upstream(self, name: str) -> list[str]:
        raise NotImplementedError

    def downstream(self, name: str) -> list[str]:
        raise NotImplementedError

    def origins(self, ref: str) -> list[str]:
        """The source columns a column is ultimately computed from, sorted."""
        raise NotImplementedError

    def impact(self, ref: str) -> list[str]:
        """Every column computed, directly or not, from this column, sorted."""
        raise NotImplementedError


def personal_data(lineage: Lineage, tagged) -> set[str]:
    """The tagged source columns and every column computed from one, except aggregates."""
    raise NotImplementedError


def read(lineage: Lineage, grants: dict[str, Grant], role: str, model: str, rows,
         personal: set[str]) -> list[dict]:
    """The rows a role may see: filtered by its row filter, personal data masked to None."""
    raise NotImplementedError


def erasure_targets(lineage: Lineage, personal: set[str]) -> list[str]:
    """The models that hold personal data, in lineage order: an erasure must rewrite each."""
    raise NotImplementedError
