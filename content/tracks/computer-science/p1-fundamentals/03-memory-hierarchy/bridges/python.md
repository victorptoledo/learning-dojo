---
from: python
status: draft
concept: "Cache-friendly Python"
from_way: "Expecting a Python list of numbers to be laid out like a C array."
native_way: "A list holds pointers to separate int or float objects, so iterating chases pointers; array.array and NumPy arrays store raw values contiguously."
sensei_tip: "For large numeric data, use NumPy (row-major by default; iterate along the last axis) or array.array; the difference is often 10–100×."
---

| In Python | Underneath |
| --- | --- |
| `list` of floats | pointers to boxed floats |
| `numpy.ndarray` | contiguous raw values |
| `a.sum(axis=1)` on C order | walks memory sequentially |
| `np.asfortranarray(a)` | column-major layout |
| `__slots__` | smaller objects, fewer cache lines |
