# Profiling and Performance: pprof, Benchmarks, Escape Analysis and the GC

Go is fast by default, and most slow Go services are slow for the same few reasons: too many
allocations, a lock everyone waits for, a missing index on the database side, or a goroutine
leak. The toolchain tells you which one, **if you measure before you change anything**.

## The loop: measure, change, measure

1. Reproduce the problem with a **benchmark** or a load test.
2. **Profile** to find where the time or memory goes.
3. Change one thing.
4. Compare benchmarks with `benchstat` over several runs.

## pprof

`pprof` reads profiles that the runtime samples cheaply enough to use in production.

| Profile | Answers |
| --- | --- |
| `cpu` | Where is CPU time spent? (sampled 100 times a second) |
| `heap` | What is allocated and still in use (`inuse_space`), or allocated in total (`alloc_space`)? |
| `allocs` | Every allocation since start, by place |
| `goroutine` | Where is every goroutine right now? (leaks, stuck requests) |
| `goroutineleak` | Goroutines blocked forever on unreachable channels or locks (Go 1.27) |
| `block` / `mutex` | Where do goroutines wait on channels and locks? (enable their rate first) |

From tests and benchmarks:

```console
$ go test -bench=Parse -cpuprofile=cpu.out -memprofile=mem.out
$ go tool pprof -http=:8080 cpu.out     # flame graph by default since Go 1.26
```

From a running service, import the handlers on an **internal** port:

```go
import _ "net/http/pprof"   // registers /debug/pprof/* on http.DefaultServeMux

go http.ListenAndServe("localhost:6060", nil)
```

```console
$ go tool pprof -http=:8080 http://localhost:6060/debug/pprof/profile?seconds=30
$ go tool pprof http://localhost:6060/debug/pprof/heap
```

In the pprof UI, **flat** is time spent in the function itself and **cum** includes what it
calls. The flame graph shows call stacks as stacked bars: wide bars are where time goes.
`top`, `list FuncName` and `peek` do the same in the terminal.

### The execution tracer

`go test -trace=trace.out` or `runtime/trace` records what every goroutine did over time:
scheduling, blocking, GC phases, system calls. `go tool trace trace.out` shows why latency
spikes when CPU profiles look flat (since Go 1.27 its web UI listens on localhost by
default). The flight recorder (`trace.FlightRecorder`, Go 1.25) keeps the last seconds in
memory, so a service can save a trace right after something went wrong.

## Allocations and escape analysis

Every heap allocation costs time to make and time for the garbage collector to find. The
compiler keeps values on the **stack** when it can prove they do not outlive the function:
**escape analysis**.

```console
$ go build -gcflags=-m ./...
./order.go:12:9: &Order{...} escapes to heap
./order.go:20:13: make([]byte, 64) does not escape
```

Common causes of escapes: returning a pointer to a local, storing a value in an interface
(including `any` arguments to `fmt`), closures captured by goroutines, and slices whose size
is unknown at compile time (Go 1.26 can keep more of those on the stack).

Cheap wins, once a profile points at them:

- **Preallocate**: `make([]T, 0, n)`, `strings.Builder.Grow`, `maps` with a size hint.
- **Avoid conversions** between `string` and `[]byte` in loops; use `strconv.AppendInt` and
  friends instead of `fmt.Sprintf` in hot paths.
- **Reuse buffers** with `sync.Pool` for short-lived, frequent objects.
- **Stream** instead of loading whole files or responses into memory (`io.Copy`, a
  `json.Decoder`).
- Check `-benchmem`: `allocs/op` is often the number to drive down.

## The garbage collector

Go's GC is **concurrent**, **non-moving** and tuned for low pause times (well under a
millisecond typically). Since Go 1.26 the default is the **Green Tea** collector, which scans
small objects with better memory locality and cuts GC CPU by roughly 10 to 40% in
GC-heavy programs.

Two knobs, both rarely needed:

- **`GOGC`** (default 100): the heap may grow by that percentage over the live heap before a
  collection. Higher means fewer collections and more memory.
- **`GOMEMLIMIT`** (Go 1.19): a soft memory limit; the GC works harder as the process nears
  it. In a container, set it a little below the memory limit to avoid out-of-memory kills.

## Profile-guided optimisation

**PGO** (Go 1.21) feeds a CPU profile from production back to the compiler, which inlines
and devirtualises the hot paths: put the profile in `default.pgo` next to `main.go` and
rebuild. Typical gains are 2 to 14% for free.

## Summary

- Measure first: benchmarks, then profiles, then benchstat to compare.
- pprof gives CPU, heap, allocation, goroutine, leak, block and mutex profiles, from tests or
  from `net/http/pprof` on an internal port; the tracer shows latency over time.
- Escape analysis (`-gcflags=-m`) explains heap allocations; preallocate, stream and reuse
  where the profile says it matters.
- The GC is concurrent with short pauses (Green Tea since 1.26); tune with `GOGC` and
  `GOMEMLIMIT` only with evidence. PGO is free speed.

## Further reading

- Diagnostics (profiling, tracing, debugging): <https://go.dev/doc/diagnostics>
- Profiling Go Programs (Go blog): <https://go.dev/blog/pprof>
- A Guide to the Go Garbage Collector: <https://go.dev/doc/gc-guide>
- Profile-guided optimization: <https://go.dev/doc/pgo>
- Package net/http/pprof: <https://pkg.go.dev/net/http/pprof>
- Go 1.26 release notes, Green Tea GC: <https://go.dev/doc/go1.26>
- roadmap.sh, Go topic "pprof": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/pprof@Blz9cpgKhuqtY75oUQw6I.md>
