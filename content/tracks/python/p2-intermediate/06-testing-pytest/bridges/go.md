---
from: go
status: draft
concept: "go test and table tests in pytest"
from_way: "func TestX(t *testing.T) with if got != want { t.Errorf }, table-driven tests with t.Run subtests, t.TempDir, t.Cleanup, t.Setenv, interfaces with hand-written fakes, and go test -cover."
native_way: "test_ functions with assert, parametrize as the table, tmp_path, fixture teardown, monkeypatch.setenv, fakes passed in (or Mock), and pytest-cov."
sensei_tip: "Go's table-driven tests are @pytest.mark.parametrize, and each case is reported separately like a t.Run subtest. Go's habit of small interfaces with fakes is the best way to test Python too."
---

| Go | pytest |
| --- | --- |
| `func TestAddVat(t *testing.T)` | `def test_add_vat():` |
| `if got != want { t.Errorf(...) }` | `assert got == want` |
| a table of cases and `t.Run(name, ...)` | `@pytest.mark.parametrize(..., ids=[...])` |
| `t.TempDir()` | `tmp_path` |
| `t.Cleanup(fn)` | code after `yield` in a fixture |
| `t.Setenv("TZ", "UTC")` | `monkeypatch.setenv("TZ", "UTC")` |
| `go test -run Vat` | `pytest -k vat` |
| `go test -cover` | `pytest --cov` |
