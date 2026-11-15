---
from: go
status: draft
concept: "Testing habits that changed"
from_way: "for i := 0; i < b.N; i++ with b.ResetTimer, tt := tt before parallel subtests, go-fuzz as an external tool, time.Sleep in tests of timeouts, context.Background in every test, and ioutil.TempDir with manual cleanup."
native_way: "b.Loop (1.24), per-iteration loop variables (1.22), native fuzzing (1.18), testing/synctest (1.25) with synctest.Sleep (1.27), t.Context (1.24), t.TempDir and t.ArtifactDir (1.26)."
sensei_tip: "Delete every time.Sleep in your tests: put the test inside synctest.Test, and the fake clock makes it fast and deterministic."
---

| Older Go | Go 1.27 |
| --- | --- |
| `b.ResetTimer(); for i := 0; i < b.N; i++` | `for b.Loop()` |
| `tt := tt` before `t.Parallel()` | not needed (1.22) |
| github.com/dvyukov/go-fuzz | `func FuzzX(f *testing.F)` |
| `time.Sleep(100 * time.Millisecond)` in tests | `synctest.Test(t, func(t *testing.T) { ... })` |
| `ctx := context.Background()` | `ctx := t.Context()` |
| `dir, _ := ioutil.TempDir(...); defer os.RemoveAll(dir)` | `dir := t.TempDir()` |
| writing debug output to /tmp | `t.ArtifactDir()` and `go test -artifacts` (1.26) |
