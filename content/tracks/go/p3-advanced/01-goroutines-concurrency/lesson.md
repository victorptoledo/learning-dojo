# Goroutines and the Scheduler

A **goroutine** is a function running concurrently with the rest of the program. Starting
one costs a few kilobytes and well under a microsecond, so a Go server can run hundreds of
thousands of them. Concurrency is part of the language, not a library.

## The go statement

```go
func main() {
	go fetch("https://go.dev")      // runs concurrently; go returns immediately
	go func() {
		fmt.Println("an anonymous goroutine")
	}()
	time.Sleep(time.Second)         // only for this demo: never synchronise with sleeps
}
```

- `go f(x)` evaluates `f` and `x` now, then runs the call in a new goroutine.
- A goroutine has **no handle**: you cannot kill it, join it or read its return value. It
  must finish on its own; you learn about it through channels, a `sync.WaitGroup` or a
  context.
- When `main` returns, the program exits and **every other goroutine stops**, finished or
  not.
- A **panic** in any goroutine that is not recovered **crashes the whole program**.

## Waiting for goroutines: sync.WaitGroup

```go
var wg sync.WaitGroup
for _, url := range urls {
	wg.Go(func() {            // Go 1.25: Add(1), go, and Done in one call
		fetch(url)
	})
}
wg.Wait()                     // blocks until every function passed to Go has returned
```

Before Go 1.25 you wrote `wg.Add(1)` before `go` and `defer wg.Done()` inside; `go fix` now
rewrites that pattern. Since Go 1.22 each loop iteration has its own `url`, so the closure
captures the right value.

For "run these, stop at the first error, return it", the `golang.org/x/sync/errgroup`
module adds error handling and cancellation on top of the same idea.

## Concurrency is not parallelism

**Concurrency** is structuring a program as independently running pieces; **parallelism**
is running them at the same instant on several cores. Go gives you the first in the code;
the runtime gives you the second when cores are available.

## How the scheduler works

The runtime multiplexes goroutines onto operating system threads with the **G-M-P** model:

```text
 G G G G G      goroutines: a stack (starts at a few KB, grows and shrinks) and a state
   │ │            queued on…
   ▼ ▼
  [P] [P]        processors: one run queue each; GOMAXPROCS of them
   │   │           each attached to…
  (M) (M)        machine threads, created by the runtime as needed
```

- **GOMAXPROCS** is how many goroutines can execute Go code at the same time. It defaults to
  the number of CPUs; since Go 1.25 it also respects a **container's CPU limit** (cgroups)
  and updates when the limit changes, which used to need the `automaxprocs` library.
- A goroutine blocked in a system call or on the network gives up its P: the network poller
  parks it and wakes it when the socket is ready. That is why blocking-style code
  (`conn.Read`) scales like asynchronous code in other languages, with no `async` keyword and
  no coloured functions.
- Idle Ps **steal** work from busy ones, and since Go 1.14 long-running loops are
  **preempted** asynchronously, so one busy goroutine cannot starve the others.

You rarely tune any of this. What matters is that blocking a goroutine is cheap, and that the
number of goroutines is not a resource you need to ration like threads.

## Goroutine leaks

A goroutine that blocks forever is a **leak**: its stack and everything it references stay
in memory, and in a server the count grows with every request.

```go
func firstResult(urls []string) string {
	results := make(chan string)        // unbuffered
	for _, url := range urls {
		go func() { results <- fetch(url) }()
	}
	return <-results                    // the other senders block forever: leak
}
```

Fixes: a buffered channel with room for every sender (`make(chan string, len(urls))`), or a
context that tells the other goroutines to stop. Rules of thumb:

- **Know how every goroutine you start will end**, and who waits for it.
- Prefer synchronous functions; let the caller add concurrency with `go`.
- Watch `runtime.NumGoroutine()` (or the `/sched/goroutines` metric) in production.

Go 1.27 made the **goroutine leak profile** generally available: `runtime/pprof` and
`/debug/pprof/goroutineleak` report goroutines blocked on a channel or lock that no running
goroutine can reach any more, using the garbage collector's reachability analysis.

## Summary

- `go f()` starts a goroutine; it has no handle, and main's return ends them all.
- Wait with `sync.WaitGroup` (`wg.Go` since 1.25) or channels; errgroup adds errors.
- The G-M-P scheduler runs goroutines on GOMAXPROCS threads at a time, container-aware
  since 1.25; blocking a goroutine is cheap.
- Every goroutine needs a way to finish; leaks are found with the goroutine (leak) profile.

## Further reading

- A Tour of Go, "Goroutines": <https://go.dev/tour/concurrency/1>
- Effective Go, "Concurrency": <https://go.dev/doc/effective_go#concurrency>
- Concurrency is not parallelism (Go blog): <https://go.dev/blog/waza-talk>
- Container-aware GOMAXPROCS (Go blog): <https://go.dev/blog/container-aware-gomaxprocs>
- Package sync, WaitGroup.Go: <https://pkg.go.dev/sync#WaitGroup.Go>
- Go 1.27 release notes, goroutine leak profile: <https://go.dev/doc/go1.27>
- roadmap.sh, Go topic "Goroutines": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/goroutines@08QOxnF3rKEDekrkX7w1j.md>
