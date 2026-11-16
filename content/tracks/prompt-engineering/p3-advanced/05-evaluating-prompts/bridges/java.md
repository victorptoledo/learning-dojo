---
from: java
status: draft
concept: "A test suite for a non-deterministic component"
from_way: "JUnit tests with exact assertions that either pass or fail, run in CI on every change."
native_way: "An evaluation set with graders that score outputs, run several times per case, reported per slice, and compared with the baseline before a prompt version ships."
sensei_tip: "Treat the evaluation set like your regression suite and the ship rule like a quality gate in CI: a red regression on a critical case blocks the merge, whatever the overall score says."
---

| Java testing | Evaluation concept |
| --- | --- |
| `assertEquals` | an exact-match grader |
| AssertJ `contains`, `matches` | contains and pattern graders |
| JSON schema validation in a test | a structure grader |
| `@Tag("slow")` | slices on test cases |
| a SonarQube quality gate | the ship rule |
