---
from: java
status: draft
concept: "Tests become measurements"
from_way: "JUnit tests that pass or fail, run in CI on every pull request."
native_way: "A golden set run as a suite that produces pass rates per slice, compared with the baseline, with a gate that fails the build on regressions."
sensei_tip: "JUnit's @ParameterizedTest with a @MethodSource over a JSONL file is a fine start; the difference is that the result is a score report, not a single green bar."
---

| Java testing | LLM evaluation |
| --- | --- |
| `@ParameterizedTest` cases | golden set cases |
| `assertEquals` | an exact metric |
| a custom `Matcher` | a rubric check or an LLM judge |
| JaCoCo threshold in CI | a score gate with a tolerance |
| test tags (`@Tag`) | slices |
