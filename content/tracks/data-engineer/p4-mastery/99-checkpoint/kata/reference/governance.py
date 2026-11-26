"""Lineage, personal data and access control over the catalog."""

import heapq

from catalog import Grant, Model


def split(column: str) -> tuple[str, str]:
    """'raw.customers.email' -> ('raw.customers', 'email'): model names may hold dots."""
    model, _, name = column.rpartition(".")
    return model, name


class Lineage:
    def __init__(self, models):
        self.models: dict[str, Model] = {}
        for model in models:
            if model.name in self.models:
                raise ValueError(f"duplicate model {model.name}")
            names = [column.name for column in model.columns]
            if len(names) != len(set(names)):
                raise ValueError(f"duplicate column in {model.name}")
            self.models[model.name] = model
        self._children: dict[str, list[str]] = {name: [] for name in self.models}
        for model in self.models.values():
            for parent in model.upstream:
                if parent not in self.models:
                    raise ValueError(f"{model.name} reads unknown model {parent}")
                self._children[parent].append(model.name)
        for model in self.models.values():
            for column in model.columns:
                for source in column.sources:
                    parent, name = split(source)
                    if parent not in model.upstream or not self._has_column(parent, name):
                        raise ValueError(f"{model.name}.{column.name} reads unknown {source}")
        self._order = self._sort()

    def _has_column(self, model: str, name: str) -> bool:
        return any(column.name == name for column in self.models[model].columns)

    def _sort(self) -> list[str]:
        waiting = {name: len(set(model.upstream)) for name, model in self.models.items()}
        ready = [name for name, count in waiting.items() if count == 0]
        heapq.heapify(ready)
        order = []
        while ready:
            name = heapq.heappop(ready)
            order.append(name)
            for child in set(self._children[name]):
                waiting[child] -= 1
                if waiting[child] == 0:
                    heapq.heappush(ready, child)
        if len(order) != len(self.models):
            raise ValueError("the lineage has a cycle")
        return order

    def order(self) -> list[str]:
        return list(self._order)

    def _closure(self, name: str, step) -> list[str]:
        if name not in self.models:
            raise KeyError(name)
        found, stack = set(), [name]
        while stack:
            for other in step(stack.pop()):
                if other not in found:
                    found.add(other)
                    stack.append(other)
        return [model for model in self._order if model in found]

    def upstream(self, name: str) -> list[str]:
        return self._closure(name, lambda model: self.models[model].upstream)

    def downstream(self, name: str) -> list[str]:
        return self._closure(name, lambda model: self._children[model])

    def column(self, ref: str):
        model, name = split(ref)
        if model not in self.models or not self._has_column(model, name):
            raise KeyError(ref)
        return next(c for c in self.models[model].columns if c.name == name)

    def origins(self, ref: str) -> list[str]:
        """The source columns a column is ultimately computed from, sorted."""
        column = self.column(ref)
        if not column.sources:
            return [ref]
        return sorted({origin for source in column.sources for origin in self.origins(source)})

    def impact(self, ref: str) -> list[str]:
        """Every column computed, directly or not, from this column, sorted."""
        self.column(ref)
        found, stack = set(), [ref]
        while stack:
            current = stack.pop()
            model, _ = split(current)
            for child in self._children[model]:
                for column in self.models[child].columns:
                    target = f"{child}.{column.name}"
                    if current in column.sources and target not in found:
                        found.add(target)
                        stack.append(target)
        return sorted(found)


def personal_data(lineage: Lineage, tagged) -> set[str]:
    """The tagged source columns and every column computed from one, except aggregates."""
    personal = set()
    for ref in tagged:
        lineage.column(ref)  # unknown columns raise KeyError
        model, _ = split(ref)
        if lineage.models[model].upstream:
            raise ValueError(f"tag source columns, not {ref}")
        personal.add(ref)
    for name in lineage.order():
        for column in lineage.models[name].columns:
            if column.transform != "aggregate" and any(s in personal for s in column.sources):
                personal.add(f"{name}.{column.name}")
    return personal


def read(lineage: Lineage, grants: dict[str, Grant], role: str, model: str, rows,
         personal: set[str]) -> list[dict]:
    """The rows a role may see: filtered by its row filter, personal data masked to None."""
    if model not in lineage.models:
        raise KeyError(model)
    grant = grants.get(role)
    if grant is None or model not in grant.models:
        raise PermissionError(f"{role} may not read {model}")
    columns = {column.name for column in lineage.models[model].columns}
    if grant.row_filter is not None:
        column, value = grant.row_filter
        if column not in columns:
            return []  # fail closed: the filter cannot be applied
        rows = [row for row in rows if row.get(column) == value]
    masked = set() if grant.pii else {
        name for name in columns if f"{model}.{name}" in personal}
    return [{key: (None if key in masked else value) for key, value in row.items()}
            for row in rows]


def erasure_targets(lineage: Lineage, personal: set[str]) -> list[str]:
    """The models that hold personal data, in lineage order: an erasure must rewrite each."""
    return [name for name in lineage.order()
            if any(f"{name}.{column.name}" in personal
                   for column in lineage.models[name].columns)]
