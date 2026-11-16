---
from: python
status: draft
concept: "Examples as data, selected per request"
from_way: "Hard-coding a few sample strings into an f-string prompt."
native_way: "A pool of labelled Example records, a selector that picks relevant, balanced examples within a token budget, and one formatting function for all of them."
sensei_tip: "Keep the example pool in a JSON Lines file next to the prompt, with a frozen dataclass to load it. Then you can test the selector, count labels and diff the pool in code review."
---

| Python | Few-shot concept |
| --- | --- |
| `@dataclass(frozen=True) class Example` | one labelled example |
| `collections.Counter(e.output for e in xs)` | checking label balance |
| `itertools` round-robin over groups | interleaving labels |
| a `format_example()` function | one consistent format |
| `pytest.mark.parametrize` | the same idea, applied to tests |
