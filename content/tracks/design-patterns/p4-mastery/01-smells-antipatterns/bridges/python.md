---
from: python
status: draft
concept: "Code smells and refactoring"
from_way: "Giant modules, dicts and tuples standing in for every record, and refactoring by search and replace."
native_way: "Characterisation tests with pytest (and snapshot plugins), dataclasses and enums for primitive obsession, type hints to make refactoring safe, and ruff or pylint to find candidates."
sensei_tip: "Add type hints before a large refactoring: mypy or pyright then find every caller you break, which is what a Java compiler gives you for free."
---

| Python | Refactoring tool or idea |
| --- | --- |
| PyCharm refactorings, rope | automated moves |
| `@dataclass`, `enum.Enum` | fix primitive obsession |
| syrupy, pytest snapshot tests | characterisation tests |
| ruff, pylint (`too-many-branches`) | smell detection |
| mypy, pyright | safety net for moves |
