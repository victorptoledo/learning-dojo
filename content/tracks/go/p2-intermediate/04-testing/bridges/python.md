---
from: python
status: draft
concept: "pytest, parametrize and property-based tests"
from_way: "pytest with plain assert and rewritten failure messages, fixtures, @pytest.mark.parametrize, unittest.mock, Hypothesis for property-based tests and pytest-benchmark."
native_way: "go test with explicit got/want messages, table-driven subtests, helpers with t.Helper and t.Cleanup, fakes instead of mocks, native fuzzing and benchmarks."
sensei_tip: "Go fuzzing is the closest thing to Hypothesis in the standard library: write the property, seed it with f.Add, and let go test -fuzz look for counter-examples."
---

| pytest | Go |
| --- | --- |
| `def test_discount():` | `func TestDiscount(t *testing.T)` |
| `assert d == 900` | `if d != 900 { t.Errorf(...) }` |
| `@pytest.mark.parametrize(...)` | a table and `t.Run(tt.name, ...)` |
| fixture with `yield` | a helper plus `t.Cleanup` |
| `tmp_path` | `t.TempDir()` |
| `monkeypatch.setenv` | `t.Setenv` |
| `unittest.mock.Mock()` | a fake struct satisfying a small interface |
| Hypothesis `@given(st.text())` | `f.Fuzz(func(t *testing.T, s string) { ... })` |
| doctest | `ExampleXxx` with `// Output:` |
