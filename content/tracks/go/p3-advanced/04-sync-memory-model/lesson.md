# sync, Atomics, the Memory Model and the Race Detector

Channels move data between goroutines. When several goroutines must **share** state, a cache,
a counter, a connection pool, you protect it with the `sync` and `sync/atomic` packages. The
**memory model** says when that protection is needed, and the **race detector** finds the
places where you forgot it.

## Data races

A **data race** happens when two goroutines access the same memory at the same time, at
least one of them writes, and nothing orders the two accesses:

```go
count := 0
var wg sync.WaitGroup
for range 1000 {
	wg.Go(func() { count++ })   // read, add, write: three steps, interleaved
}
wg.Wait()
fmt.Println(count)              // anything up to 1000, different on every run
```

A racy program has no defined behaviour you can rely on: lost updates, torn values, a map
corrupted badly enough that the runtime stops with `fatal error: concurrent map writes`.

## The memory model in one page

The Go memory model defines **happens-before**: if event A happens before event B, B sees
A's writes. Within one goroutine, statements happen in program order. Between goroutines you
only get ordering from **synchronisation**:

- a send on a channel happens before the matching receive completes; closing a channel
  happens before a receive that returns because it is closed;
- `Unlock` of a mutex happens before the next `Lock` returns;
- `wg.Done` (or the end of a function given to `wg.Go`) happens before `wg.Wait` returns;
- `once.Do(f)`: the return of `f` happens before any `Do` returns;
- atomic operations are ordered like sequentially consistent operations;
- the `go` statement happens before the new goroutine starts.

The guarantee is **DRF-SC**: a program **without data races** behaves as if all goroutines
ran interleaved on one CPU, in some order. Write race-free code and you can reason simply;
"benign" races do not exist in Go.

## Mutexes

```go
type Cache struct {
	mu    sync.Mutex            // guards items
	items map[string]string
}

func (c *Cache) Get(key string) (string, bool) {
	c.mu.Lock()
	defer c.mu.Unlock()
	v, ok := c.items[key]
	return v, ok
}

func (c *Cache) Set(key, value string) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if c.items == nil {
		c.items = make(map[string]string)
	}
	c.items[key] = value
}
```

- Put the mutex **next to the fields it guards** and say so in a comment.
- Lock, `defer Unlock`, keep the critical section short, and **never** call slow I/O or
  unknown callbacks while holding a lock.
- A mutex **must not be copied** after use: use pointer receivers. `go vet`'s `copylocks`
  check catches copies.
- `sync.RWMutex` allows many readers or one writer. It only helps when reads dominate and
  the critical section is not tiny; measure before switching.
- Mutexes are not reentrant: locking twice in the same goroutine deadlocks.

## Atomics

For a single counter or flag, `sync/atomic` types are simpler and faster than a mutex:

```go
var requests atomic.Int64
requests.Add(1)
n := requests.Load()

var ready atomic.Bool
ready.Store(true)

var config atomic.Pointer[Config]   // swap a whole immutable value at once
config.Store(&Config{Limit: 10})
```

Atomics protect **one variable**. As soon as two fields must change together, use a mutex.

## The rest of sync

| Tool | For |
| --- | --- |
| `sync.WaitGroup` | wait for a group of goroutines (`wg.Go`, Go 1.25) |
| `sync.Once`, `sync.OnceFunc`, `sync.OnceValue`, `sync.OnceValues` | run initialisation exactly once, safely, from any goroutine |
| `sync.Map` | a concurrent map for two cases: keys written once and read many times, or goroutines working on disjoint keys. Otherwise a map plus a mutex is clearer and often faster |
| `sync.Pool` | reuse temporary objects (buffers) to reduce garbage; items may be dropped at any GC |
| `sync.Cond` | rarely needed; channels usually express "wait until" better |

```go
var loadConfig = sync.OnceValues(func() (*Config, error) {
	return readConfig("config.yaml")
})

cfg, err := loadConfig()   // the file is read once, by whoever calls first
```

## The race detector

```console
$ go test -race ./...
$ go run -race .
```

`-race` instruments every memory access. When two goroutines touch the same memory without
a happens-before edge, it prints both stacks:

```text
WARNING: DATA RACE
Write at 0x00c000014090 by goroutine 8:
  main.main.func1()   /app/main.go:12 +0x44
Previous write at 0x00c000014090 by goroutine 7:
  main.main.func1()   /app/main.go:12 +0x44
```

- It finds races that **happen during the run**, never false positives; code paths the tests
  do not execute are not checked, so tests must exercise the concurrent paths.
- It costs roughly 5 to 10 times the memory and 2 to 20 times the CPU: use it in tests and
  CI (the dojo runs every Go kata with it), and in a canary if you can afford it.

## Summary

- A data race (concurrent access, one write, no ordering) has undefined results; maps are
  not safe for concurrent writes.
- Happens-before comes from channels, mutexes, WaitGroups, Once and atomics; race-free
  programs behave sequentially consistently.
- Mutex next to the data, short critical sections, never copied; RWMutex only when measured.
- Atomic types for single values; Once and OnceValue for lazy initialisation; sync.Map and
  sync.Pool for their narrow cases.
- Run tests with `-race`, always.

## Further reading

- The Go Memory Model: <https://go.dev/ref/mem>
- Data Race Detector: <https://go.dev/doc/articles/race_detector>
- Package sync: <https://pkg.go.dev/sync>
- Package sync/atomic: <https://pkg.go.dev/sync/atomic>
- Introducing the Go Race Detector (Go blog): <https://go.dev/blog/race-detector>
- roadmap.sh, Go topic "Mutexes": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/mutexes@6ydgmac11Zu9Ithe0kKj9.md>
