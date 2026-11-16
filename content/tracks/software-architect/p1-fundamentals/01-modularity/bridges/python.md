---
from: python
status: draft
concept: "Modularity and package dependencies"
from_way: "A `utils.py` and a `models.py` imported by every package, and circular imports worked around with imports inside functions."
native_way: "Packages that hide a decision each, a leading underscore for private modules, `__all__` for the public interface, and `import-linter` contracts against cycles and forbidden imports."
sensei_tip: "An import inside a function to dodge a circular import is a cycle you have hidden, not removed. Break it by moving the shared piece or inverting the dependency."
---

| Python | Modularity idea |
| --- | --- |
| `_private.py`, `_helper()` | information hiding by convention |
| `__all__`, re-exports in `__init__.py` | a package's published interface |
| `import-linter` independence and layers contracts | acyclic and allowed dependencies as checks |
| `pydeps`, `grimp` | the import graph, for metrics |
| `utils.py` used everywhere | the zone of pain |
| typing `Protocol` | an abstraction a stable package can own |
