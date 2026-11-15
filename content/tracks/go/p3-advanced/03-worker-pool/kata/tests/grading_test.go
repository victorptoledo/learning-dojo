package pool_test

// These tests run inside testing/synctest bubbles: time.Sleep uses a fake clock that jumps
// forward when every goroutine is blocked, and a bubble fails if goroutines are left blocked
// when the test ends (a leak).

import (
	"errors"
	"slices"
	"strings"
	"sync/atomic"
	"testing"
	"testing/synctest"
	"time"

	"dojo/pool"
)

// tracker counts how many calls run at the same time.
type tracker struct {
	active, peak, calls atomic.Int64
}

func (tr *tracker) enter() {
	tr.calls.Add(1)
	now := tr.active.Add(1)
	for {
		peak := tr.peak.Load()
		if now <= peak || tr.peak.CompareAndSwap(peak, now) {
			return
		}
	}
}

func (tr *tracker) leave() { tr.active.Add(-1) }

func TestMapKeepsJobOrderWhenLaterJobsFinishFirst(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		jobs := []int{50, 40, 30, 20, 10}
		got, err := pool.Map(jobs, 5, func(ms int) (int, error) {
			time.Sleep(time.Duration(ms) * time.Millisecond)
			return ms * 2, nil
		})
		if err != nil {
			t.Fatalf("Map = %v", err)
		}
		if want := []int{100, 80, 60, 40, 20}; !slices.Equal(got, want) {
			t.Errorf("Map = %v, want %v", got, want)
		}
	})
}

func TestMapRunsExactlyWorkersJobsAtOnce(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var tr tracker
		start := time.Now()
		_, err := pool.Map(make([]int, 20), 4, func(int) (int, error) {
			tr.enter()
			defer tr.leave()
			time.Sleep(10 * time.Millisecond)
			return 0, nil
		})
		if err != nil {
			t.Fatalf("Map = %v", err)
		}
		if peak := tr.peak.Load(); peak != 4 {
			t.Errorf("at most %d jobs ran at once, want exactly 4", peak)
		}
		if elapsed := time.Since(start); elapsed != 50*time.Millisecond {
			t.Errorf("20 jobs of 10ms on 4 workers took %v, want 50ms", elapsed)
		}
	})
}

func TestMapWithFewerThanOneWorkerUsesOne(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var tr tracker
		got, err := pool.Map([]int{1, 2, 3}, 0, func(n int) (int, error) {
			tr.enter()
			defer tr.leave()
			time.Sleep(time.Millisecond)
			return n + 1, nil
		})
		if err != nil || !slices.Equal(got, []int{2, 3, 4}) {
			t.Errorf("Map(workers 0) = %v, %v; want [2 3 4], nil", got, err)
		}
		if peak := tr.peak.Load(); peak != 1 {
			t.Errorf("workers 0 ran %d jobs at once, want 1", peak)
		}
	})
}

var errOdd = errors.New("odd number")

func TestMapRunsEveryJobAndJoinsErrorsInJobOrder(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var tr tracker
		got, err := pool.Map([]int{1, 2, 3, 4, 5, 6}, 3, func(n int) (int, error) {
			tr.enter()
			defer tr.leave()
			time.Sleep(time.Duration(7-n) * time.Millisecond)
			if n%2 == 1 {
				return 0, errOdd
			}
			return n * 10, nil
		})
		if calls := tr.calls.Load(); calls != 6 {
			t.Errorf("fn ran %d times, want 6: every job runs even after a failure", calls)
		}
		if !errors.Is(err, errOdd) {
			t.Fatalf("Map error = %v, want one wrapping errOdd", err)
		}
		message := err.Error()
		first, second, third := strings.Index(message, "job 0: "), strings.Index(message, "job 2: "), strings.Index(message, "job 4: ")
		if first < 0 || second < first || third < second {
			t.Errorf("Map error = %q, want \"job 0: ...\", \"job 2: ...\" and \"job 4: ...\" in that order", message)
		}
		if want := []int{0, 20, 0, 40, 0, 60}; !slices.Equal(got, want) {
			t.Errorf("Map results = %v, want %v", got, want)
		}
	})
}

func TestMapWithNoJobs(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		got, err := pool.Map(nil, 4, func(n int) (int, error) { return n, nil })
		if len(got) != 0 || err != nil {
			t.Errorf("Map(nil) = %v, %v; want no results and nil", got, err)
		}
	})
}

func TestGenerateSendsInOrderAndCloses(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var got []int
		for n := range pool.Generate(3, 1, 2) {
			got = append(got, n)
		}
		if want := []int{3, 1, 2}; !slices.Equal(got, want) {
			t.Errorf("values from Generate(3, 1, 2) = %v, want %v", got, want)
		}
	})
}

func TestSquareFansOutAndClosesItsOutput(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		var got []int
		for n := range pool.Square(pool.Generate(1, 2, 3, 4, 5, 6, 7, 8), 3) {
			got = append(got, n)
		}
		slices.Sort(got)
		if want := []int{1, 4, 9, 16, 25, 36, 49, 64}; !slices.Equal(got, want) {
			t.Errorf("sorted squares = %v, want %v", got, want)
		}
	})
}

func TestSquareWorkersShareTheInput(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		in := make(chan int)
		out := pool.Square(in, 3)
		for n := range 3 {
			in <- n // three sends succeed only if three workers are reading
		}
		synctest.Wait() // every worker is now blocked sending its square
		close(in)
		count := 0
		for range out {
			count++
		}
		if count != 3 {
			t.Errorf("received %d squares, want 3", count)
		}
	})
}

func TestSumOfAPipeline(t *testing.T) {
	synctest.Test(t, func(t *testing.T) {
		if got := pool.Sum(pool.Square(pool.Generate(1, 2, 3, 4), 2)); got != 30 {
			t.Errorf("Sum(Square(Generate(1..4))) = %d, want 30", got)
		}
		if got := pool.Sum(pool.Generate()); got != 0 {
			t.Errorf("Sum(Generate()) = %d, want 0", got)
		}
	})
}
