---
from: javascript
status: draft
concept: "Snapshot and visual-regression testing"
from_way: "Jest or Vitest suites, snapshot tests and visual-regression checks that compare a new render against a baseline."
native_way: "Run the prompt over a fixed set of cases, grade each output, and compare against the baseline version case by case: fixes, regressions and pass rate per slice."
sensei_tip: "A prompt change is like a CSS change: it looks fine on the page you were editing and breaks a page you did not open. The evaluation set is your visual-regression suite."
---

| JavaScript | Evaluation concept |
| --- | --- |
| `test.each(cases)` | the test set |
| `describe` blocks per feature | slices |
| `toEqual` / `toMatch` | exact and pattern graders |
| visual-regression baseline | the baseline prompt's results |
| a failing check on the PR | the ship rule |
