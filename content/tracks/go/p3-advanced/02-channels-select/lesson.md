# Channels, select and Concurrency Patterns

A **channel** is a typed pipe between goroutines: one sends a value, another receives it,
and the hand-over synchronises them. Go's motto: *do not communicate by sharing memory;
share memory by communicating*.

## Sending, receiving, closing

```go
ch := make(chan int)        // unbuffered
go func() {
	ch <- 42                // send: blocks until someone receives
}()
v := <-ch                   // receive: blocks until someone sends
```

| Operation | Open channel | Closed channel | nil channel |
| --- | --- | --- | --- |
| `ch <- v` | blocks until received (or buffer space) | **panics** | blocks forever |
| `<-ch` | blocks until a value arrives | returns the zero value at once | blocks forever |
| `v, ok := <-ch` | `ok == true` | `ok == false` | blocks forever |
| `close(ch)` | closes | **panics** | **panics** |

- **Unbuffered** (`make(chan T)`): sender and receiver meet; the send completes only when
  the value is taken. It is a synchronisation point.
- **Buffered** (`make(chan T, n)`): up to `n` values wait in a queue; sends block only when
  it is full. Use a buffer for a known number of results or to absorb bursts, not to hide
  a design problem.
- **Close** means "no more values". Only the **sender** closes, and only when receivers
  need to know (to end a `range`). Channels do not need closing to be garbage collected.

```go
for v := range ch {         // receives until ch is closed and drained
	fmt.Println(v)
}
```

Direction in types documents and enforces roles: `chan<- int` can only send, `<-chan int`
can only receive. A function that produces values returns `<-chan T`.

## select

`select` waits on several channel operations and runs the first one that is ready (at
random if several are):

```go
select {
case msg := <-messages:
	handle(msg)
case results <- next:
	next = produce()
case <-time.After(2 * time.Second):
	return errors.New("timed out")
case <-ctx.Done():
	return ctx.Err()
}
```

- A `default` case makes the select **non-blocking**: it runs when nothing else is ready.
- A **nil channel** is never ready, so setting a channel variable to `nil` disables its case;
  that is how you merge two channels until both are closed.
- `time.After` creates a timer each time; in a long loop prefer one `time.NewTimer` or
  `time.Ticker` and `Stop` it. Since Go 1.23 unused timers are garbage collected and timer
  channels are unbuffered, so `Reset` and `Stop` no longer deliver stale values.

## Patterns

### Generator and pipeline

Each stage is a goroutine that reads from one channel and writes to the next, and closes its
output when its input ends:

```go
func gen(nums ...int) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out)
		for _, n := range nums {
			out <- n
		}
	}()
	return out
}

func square(in <-chan int) <-chan int {
	out := make(chan int)
	go func() {
		defer close(out)
		for n := range in {
			out <- n * n
		}
	}()
	return out
}

for v := range square(gen(1, 2, 3)) { fmt.Println(v) }   // 1 4 9
```

### Fan-out, fan-in and worker pools

**Fan-out**: several goroutines read from the same channel, so work spreads among them.
**Fan-in**: their outputs merge into one channel, which is closed once **all** of them are
done; a WaitGroup tells you when:

```go
func workers(in <-chan Job, n int) <-chan Result {
	out := make(chan Result)
	var wg sync.WaitGroup
	for range n {
		wg.Go(func() {
			for job := range in {
				out <- process(job)
			}
		})
	}
	go func() {
		wg.Wait()
		close(out)        // after the last worker, never before
	}()
	return out
}
```

A fixed number of workers bounds concurrency: never more than `n` jobs at once, however many
are queued.

### Semaphore

A buffered channel of empty structs limits how many goroutines do something at once:

```go
sem := make(chan struct{}, 3)
for _, url := range urls {
	wg.Go(func() {
		sem <- struct{}{}        // take a slot; blocks when 3 are busy
		defer func() { <-sem }() // give it back
		fetch(url)
	})
}
```

### Done channels

A channel that is only ever closed broadcasts an event to every receiver at once: closing
wakes them all. `ctx.Done()` is exactly that, and the context lesson builds on it.

## Channels or mutexes?

| Use a channel to | Use a mutex to |
| --- | --- |
| pass ownership of data to another goroutine | protect a shared cache, counter or map |
| distribute work and collect results | guard a struct's invariants inside its methods |
| signal events (done, timeouts, cancellation) | keep critical sections short and local |

Both are fine Go. Channels shine for **flow**, mutexes for **state**.

## Summary

- Unbuffered channels synchronise; buffered channels queue up to their capacity.
- Only senders close, closing a closed channel panics, receiving from a closed one returns
  zero values; nil channels block forever.
- `select` waits on several operations, with `default` for non-blocking, timers for
  timeouts and `ctx.Done()` for cancellation.
- Pipelines, fan-out and fan-in with a WaitGroup that closes the output, worker pools and
  semaphores bound concurrency.

## Further reading

- A Tour of Go, "Channels" and "Select": <https://go.dev/tour/concurrency/2>
- Go Concurrency Patterns: Pipelines and cancellation (Go blog): <https://go.dev/blog/pipelines>
- Share Memory By Communicating (Go blog): <https://go.dev/blog/codelab-share>
- The Go spec, "Select statements": <https://go.dev/ref/spec#Select_statements>
- Effective Go, "Channels": <https://go.dev/doc/effective_go#channels>
- roadmap.sh, Go topic "Select Statement": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/golang/content/select-statement@RBr1uAdngIsvSpIdHHOyV.md>
