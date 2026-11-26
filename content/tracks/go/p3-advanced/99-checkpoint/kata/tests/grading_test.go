package batch_test

import (
	"context"
	"errors"
	"fmt"
	"slices"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"testing/synctest"
	"time"

	"dojo/batch"
)

// recorder keeps every batch it receives and when it came, safe to read from the test.
type recorder struct {
	mu      sync.Mutex
	batches [][]int
	at      []time.Duration
	start   time.Time
}

func newRecorder() *recorder { return &recorder{start: time.Now()} }

func (r *recorder) flush(_ context.Context, items []int) error {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.batches = append(r.batches, items)
	r.at = append(r.at, time.Since(r.start))
	return nil
}

func (r *recorder) snapshot() ([][]int, []time.Duration) {
	r.mu.Lock()
	defer r.mu.Unlock()
	out := make([][]int, len(r.batches))
	for i, b := range r.batches {
		out[i] = slices.Clone(b)
	}
	return out, slices.Clone(r.at)
}

func addAll(t *testing.T, b *batch.Batcher[int], items ...int) {
	t.Helper()
	for _, item := range items {
		if err := b.Add(context.Background(), item); err != nil {
			t.Fatalf("Add(%d) = %v, want nil", item, err)
		}
	}
}

func TestFullBatchesInOrderWithStats(t *testing.T) {
	r := newRecorder()
	b := batch.Start(context.Background(), batch.Config{Size: 3}, r.flush)
	addAll(t, b, 1, 2, 3, 4, 5, 6, 7)
	if err := b.Close(); err != nil {
		t.Fatalf("Close = %v, want nil", err)
	}
	got, _ := r.snapshot()
	want := [][]int{{1, 2, 3}, {4, 5, 6}, {7}}
	if !slices.EqualFunc(got, want, slices.Equal) {
		t.Errorf("batches = %v, want %v (full batches, then the rest on Close)", got, want)
	}
	if s := b.Stats(); s != (batch.Stats{Items: 7, Batches: 3}) {
		t.Errorf("Stats = %+v, want {Items:7 Batches:3 Failed:0}", s)
	}
}

func TestSizeBelowOneMeansOne(t *testing.T) {
	r := newRecorder()
	b := batch.Start(context.Background(), batch.Config{Size: 0}, r.flush)
	addAll(t, b, 1, 2)
	b.Close()
	got, _ := r.snapshot()
	if want := [][]int{{1}, {2}}; !slices.EqualFunc(got, want, slices.Equal) {
		t.Errorf("batches with Size 0 = %v, want %v", got, want)
	}
}

func TestFlushedSlicesAreNotReused(t *testing.T) {
	var kept [][]int // the flush keeps the slices it was given, without copying them
	b := batch.Start(context.Background(), batch.Config{Size: 2}, func(_ context.Context, items []int) error {
		kept = append(kept, items)
		return nil
	})
	addAll(t, b, 1, 2, 3, 4, 5, 6)
	b.Close()
	want := [][]int{{1, 2}, {3, 4}, {5, 6}}
	if !slices.EqualFunc(kept, want, slices.Equal) {
		t.Errorf("kept batches = %v, want %v: a flushed slice belongs to the flush function, so the next batch needs a new array", kept, want)
	}
}

func TestWaitCountsFromTheFirstItem(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		r := newRecorder()
		b := batch.Start(context.Background(), batch.Config{Size: 10, Wait: time.Second}, r.flush)
		defer b.Close() // a failed check must not leave the loop blocked in the bubble
		addAll(t, b, 1)
		time.Sleep(600 * time.Millisecond)
		addAll(t, b, 2) // must not restart the clock
		time.Sleep(399 * time.Millisecond)
		synctest.Wait()
		if got, _ := r.snapshot(); len(got) != 0 {
			t.Fatalf("flushed %v after 999ms, want nothing before Wait", got)
		}
		time.Sleep(time.Millisecond)
		synctest.Wait()
		got, at := r.snapshot()
		if want := [][]int{{1, 2}}; !slices.EqualFunc(got, want, slices.Equal) {
			t.Fatalf("batches after 1s = %v, want %v: Wait counts from the first item of the batch, not the last", got, want)
		}
		if at[0] != time.Second {
			t.Errorf("flushed at %v, want 1s", at[0])
		}
		// A new batch starts a new clock.
		addAll(t, b, 3)
		time.Sleep(time.Second)
		synctest.Wait()
		if got, _ := r.snapshot(); len(got) != 2 || !slices.Equal(got[1], []int{3}) {
			t.Errorf("batches = %v, want a second batch [3] one second after it started", got)
		}
		if err := b.Close(); err != nil {
			t.Errorf("Close = %v, want nil", err)
		}
	})
}

func TestFullBatchStopsTheClock(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		r := newRecorder()
		b := batch.Start(context.Background(), batch.Config{Size: 2, Wait: time.Second}, r.flush)
		defer b.Close()    // a failed check must not leave the loop blocked in the bubble
		addAll(t, b, 1, 2) // full at once
		time.Sleep(500 * time.Millisecond)
		addAll(t, b, 3)
		time.Sleep(700 * time.Millisecond) // 1.2s after item 1, 0.7s after item 3
		synctest.Wait()
		got, _ := r.snapshot()
		if want := [][]int{{1, 2}}; !slices.EqualFunc(got, want, slices.Equal) {
			t.Errorf("batches = %v, want %v: the timer of a flushed batch must not flush the next one early", got, want)
		}
		b.Close()
	})
}

func TestNoWaitMeansNoTimer(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		r := newRecorder()
		b := batch.Start(context.Background(), batch.Config{Size: 5}, r.flush)
		defer b.Close() // a failed check must not leave the loop blocked in the bubble
		addAll(t, b, 1, 2)
		time.Sleep(time.Hour)
		synctest.Wait()
		if got, _ := r.snapshot(); len(got) != 0 {
			t.Fatalf("flushed %v with Wait 0, want only Size and Close to flush", got)
		}
		b.Close()
		if got, _ := r.snapshot(); !slices.EqualFunc(got, [][]int{{1, 2}}, slices.Equal) {
			t.Errorf("batches after Close = %v, want [[1 2]]", got)
		}
	})
}

func TestCloseIsIdempotentAndRejectsLaterAdds(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		boom := errors.New("disk full")
		b := batch.Start(context.Background(), batch.Config{Size: 1}, func(context.Context, []int) error { return boom })
		addAll(t, b, 1)
		first := b.Close()
		if !errors.Is(first, boom) {
			t.Fatalf("Close = %v, want it to wrap the flush error", first)
		}
		var wg sync.WaitGroup
		for range 3 {
			wg.Go(func() {
				if again := b.Close(); !errors.Is(again, boom) {
					t.Errorf("Close again = %v, want the same flush error", again)
				}
			})
		}
		wg.Wait()
		if err := b.Add(context.Background(), 2); !errors.Is(err, batch.ErrClosed) {
			t.Errorf("Add after Close = %v, want ErrClosed", err)
		}
	})
}

func TestFlushErrorsAreCountedWrappedAndJoined(t *testing.T) {
	errOdd := errors.New("odd batch")
	n := 0
	b := batch.Start(context.Background(), batch.Config{Size: 1}, func(context.Context, []int) error {
		n++
		if n%2 == 1 {
			return fmt.Errorf("write: %w", errOdd)
		}
		return nil
	})
	addAll(t, b, 10, 20, 30, 40, 50) // batches 1, 3 and 5 fail; the batcher keeps going
	err := b.Close()
	if !errors.Is(err, errOdd) {
		t.Fatalf("Close = %v, want it to wrap the flush errors", err)
	}
	lines := strings.Split(err.Error(), "\n")
	for _, part := range []string{"batch 1: write: odd batch", "batch 3: write: odd batch", "batch 5: write: odd batch"} {
		if !slices.Contains(lines, part) {
			t.Errorf("Close error %q has no line %q", err, part)
		}
	}
	if len(lines) != 3 {
		t.Errorf("Close error has %d lines, want 3 (one per failed batch)", len(lines))
	}
	if s := b.Stats(); s != (batch.Stats{Items: 5, Batches: 5, Failed: 3}) {
		t.Errorf("Stats = %+v, want {Items:5 Batches:5 Failed:3}", s)
	}
}

func TestContextEndFlushesWithALiveContextAndStops(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		ctx, cancel := context.WithCancel(context.Background())
		var finalErr error
		var got [][]int
		b := batch.Start(ctx, batch.Config{Size: 10}, func(fctx context.Context, items []int) error {
			finalErr = fctx.Err()
			got = append(got, items)
			return nil
		})
		addAll(t, b, 1, 2)
		cancel()
		synctest.Wait() // the loop must notice and flush on its own, without Close
		if !slices.EqualFunc(got, [][]int{{1, 2}}, slices.Equal) {
			t.Fatalf("batches after cancel = %v, want [[1 2]] flushed without Close", got)
		}
		if finalErr != nil {
			t.Errorf("the last flush got a context with Err() = %v, want a live one (context.WithoutCancel) so the last batch is not lost", finalErr)
		}
		if err := b.Add(context.Background(), 3); !errors.Is(err, batch.ErrClosed) {
			t.Errorf("Add after the context ended = %v, want ErrClosed", err)
		}
		if err := b.Close(); err != nil {
			t.Errorf("Close after the context ended = %v, want nil", err)
		}
	}) // synctest fails here if the loop goroutine is still alive
}

func TestAddGivesUpWhenItsContextEnds(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var got [][]int
		b := batch.Start(context.Background(), batch.Config{Size: 1}, func(_ context.Context, items []int) error {
			time.Sleep(time.Second) // a slow flush: Add must wait (backpressure)
			got = append(got, items)
			return nil
		})
		addAll(t, b, 1) // accepted; the loop is now flushing for a second
		ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
		defer cancel()
		start := time.Now()
		err := b.Add(ctx, 2)
		if !errors.Is(err, context.DeadlineExceeded) {
			t.Fatalf("Add during a slow flush with a 100ms deadline = %v, want context.DeadlineExceeded", err)
		}
		if waited := time.Since(start); waited != 100*time.Millisecond {
			t.Errorf("Add returned after %v, want 100ms", waited)
		}
		b.Close()
		if !slices.EqualFunc(got, [][]int{{1}}, slices.Equal) {
			t.Errorf("batches = %v, want [[1]]: an item whose Add failed must not be flushed", got)
		}
	})
}

func TestConcurrentAddsAndCloseNeverPanic(t *testing.T) {
	var flushed atomic.Int64
	var inFlight, maxInFlight atomic.Int32
	b := batch.Start(context.Background(), batch.Config{Size: 7, Wait: time.Millisecond}, func(_ context.Context, items []int) error {
		if n := inFlight.Add(1); n > maxInFlight.Load() {
			maxInFlight.Store(n)
		}
		time.Sleep(50 * time.Microsecond)
		flushed.Add(int64(len(items)))
		inFlight.Add(-1)
		return nil
	})
	var accepted atomic.Int64
	var wg sync.WaitGroup
	for g := range 40 {
		wg.Go(func() {
			for i := range 25 {
				err := b.Add(context.Background(), g*100+i)
				switch {
				case err == nil:
					accepted.Add(1)
				case errors.Is(err, batch.ErrClosed):
				default:
					t.Errorf("Add = %v, want nil or ErrClosed", err)
				}
				_ = b.Stats() // Stats is read while the loop writes it
			}
		})
	}
	time.Sleep(2 * time.Millisecond)
	if err := b.Close(); err != nil {
		t.Errorf("Close = %v, want nil", err)
	}
	wg.Wait()
	if accepted.Load() != flushed.Load() {
		t.Errorf("%d Adds returned nil but %d items were flushed: every accepted item must be flushed once before Close returns", accepted.Load(), flushed.Load())
	}
	if s := b.Stats(); s.Items != flushed.Load() {
		t.Errorf("Stats.Items = %d, want %d", s.Items, flushed.Load())
	}
	if maxInFlight.Load() > 1 {
		t.Errorf("%d flushes ran at the same time, want one at a time", maxInFlight.Load())
	}
}
