# Testing: Table Tests, Fuzzing and Benchmarks

Testing is built into Go: the `testing` package and `go test` cover unit tests, examples,
benchmarks, fuzzing and coverage, with no framework to install. The style is plain Go code:
`if got != want { t.Errorf(...) }`.

## Test files and functions

Tests live next to the code, in files ending in `_test.go`, which `go build` ignores:

```go
// price_test.go
package price_test            // a black-box test: only the exported API

import (
	"testing"

	"example.com/shop/price"
)

func TestDiscount(t *testing.T) {
	got := price.Discount(1000, 10)
	if got != 900 {
		t.Errorf("Discount(1000, 10) = %d, want 900", got)
	}
}
```

- A test is `func TestXxx(t *testing.T)`. `t.Errorf` records a failure and continues;
  `t.Fatalf` stops this test (use it when the rest cannot run, such as after a failed setup).
- `package price` (white-box) can test unexported code; `package price_test` (black-box)
  tests what users see. Both can sit in the same folder.
- Write messages as `Func(input) = got, want want`: the reader should not need the code.
- `t.Helper()` in a helper makes failures point at the caller's line. `t.Cleanup(f)` registers
  teardown, `t.TempDir()` gives a folder deleted afterwards, `t.Context()` (1.24) a context
  cancelled when the test ends, and `t.Setenv` sets a variable for this test only.

```console
$ go test ./...                    # every package
$ go test -run 'TestDiscount/zero' # filter by test and subtest name (regular expressions)
$ go test -race -count=1 ./...     # race detector, no cached results
$ go test -cover ./...             # coverage percentage
```

## Table-driven tests and subtests

The most common Go test shape is a **table** of cases and a loop with `t.Run`:

```go
func TestDiscount(t *testing.T) {
	tests := []struct {
		name    string
		cents   int64
		percent int
		want    int64
	}{
		{"ten percent", 1000, 10, 900},
		{"zero percent", 1000, 0, 1000},
		{"rounds down", 999, 10, 900},
		{"full discount", 500, 100, 0},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := price.Discount(tt.cents, tt.percent); got != tt.want {
				t.Errorf("Discount(%d, %d) = %d, want %d", tt.cents, tt.percent, got, tt.want)
			}
		})
	}
}
```

Each case is a **subtest** with its own name (`TestDiscount/rounds_down`), so it can fail on
its own, be run alone with `-run`, and call `t.Parallel()`. Adding a case is one line.
Since Go 1.22 the loop variable `tt` is fresh in every iteration, so parallel subtests are
safe without the old `tt := tt`.

Compare structs with `==` when they are comparable, slices and maps with `slices.Equal`,
`maps.Equal` or `reflect.DeepEqual`; the `github.com/google/go-cmp` module prints readable
diffs when you need them.

### Fakes, not mocking frameworks

Code that depends on small interfaces is easy to test with a hand-written fake:

```go
type fakeClock struct{ now time.Time }

func (f fakeClock) Now() time.Time { return f.now }
```

For HTTP, `net/http/httptest` gives you `NewRecorder` (to call a handler directly) and
`NewServer` (a real server on a local port). Test data files go in a `testdata/` folder,
which the go command ignores; "golden files" there hold expected outputs.

### Examples

An `ExampleXxx` function is documentation **and** a test: `go test` runs it and compares its
standard output with the `// Output:` comment.

```go
func ExampleDiscount() {
	fmt.Println(price.Discount(1000, 10))
	// Output: 900
}
```

## Fuzzing

A **fuzz test** lets the toolchain generate inputs to find crashes and broken properties:

```go
func FuzzParseCents(f *testing.F) {
	for _, seed := range []string{"0", "10.5", "1234.99"} {
		f.Add(seed)                                   // the seed corpus
	}
	f.Fuzz(func(t *testing.T, text string) {
		cents, err := ParseCents(text)
		if err != nil {
			return                                    // invalid input is fine, crashing is not
		}
		if back, _ := ParseCents(FormatCents(cents)); back != cents {
			t.Errorf("round trip of %q: %d became %d", text, cents, back)
		}
	})
}
```

`go test` runs the seeds as normal tests. `go test -fuzz=FuzzParseCents -fuzztime=30s`
mutates inputs; a failing input is saved under `testdata/fuzz/FuzzParseCents/` and replayed by
every later `go test`, so a bug stays fixed. Fuzz **properties**: round trips, "never
panics", agreement with a slower reference implementation.

## Benchmarks

```go
func BenchmarkWords(b *testing.B) {
	text := strings.Repeat("the quick brown fox ", 1000)
	for b.Loop() {                     // Go 1.24: runs the body as many times as needed
		Words(text)
	}
}
```

```console
$ go test -bench=Words -benchmem
BenchmarkWords-8   4210   281340 ns/op   164024 B/op   4012 allocs/op
```

`b.Loop()` replaces the old `for i := 0; i < b.N; i++` loop: setup before the loop is not
timed, and the compiler cannot optimise the body away. `-benchmem` adds bytes and
allocations per operation. Compare two versions with `benchstat old.txt new.txt` over
several runs (`-count=10`), never from a single run.

## Testing concurrent and timed code

`testing/synctest` (Go 1.25) runs a test in a **bubble** with a fake clock: `time.Sleep`,
timers and timeouts advance instantly once every goroutine in the bubble is blocked, and
`synctest.Wait()` waits until they are. A test of a 5-second timeout then takes
microseconds and never flakes. Go 1.27 adds `synctest.Sleep` and an in-memory
`httptest.NewTestServer` that works inside a bubble.

Always run tests with `-race` in CI; the concurrency phase explains what it finds.

## Summary

- `_test.go` files, `TestXxx(t *testing.T)`, `t.Errorf` to continue and `t.Fatalf` to stop.
- Table-driven tests with `t.Run` subtests; clear `got, want` messages; fakes over mocks.
- Examples document and test; fuzz tests (`f.Add`, `f.Fuzz`, `-fuzz`) find edge cases and keep
  them in `testdata/fuzz`.
- Benchmarks with `b.Loop()` and `-benchmem`, compared with benchstat.
- `testing/synctest` for time and concurrency; `-race` always.

## Further reading

- Package testing: <https://pkg.go.dev/testing>
- Go wiki, "Table-driven tests": <https://go.dev/wiki/TableDrivenTests>
- Tutorial, "Getting started with fuzzing": <https://go.dev/doc/tutorial/fuzz>
- Go Fuzzing reference: <https://go.dev/doc/security/fuzz/>
- More predictable benchmarking with testing.B.Loop (Go blog): <https://go.dev/blog/testing-b-loop>
- Testing concurrent code with testing/synctest (Go blog): <https://go.dev/blog/synctest>
- roadmap.sh, Go topic "Table-driven Tests": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/table-driven-tests@6-mr65JzXbRzIukE7jzoZ.md>
